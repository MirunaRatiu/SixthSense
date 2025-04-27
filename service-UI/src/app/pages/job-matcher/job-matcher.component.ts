import { Component,  OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { FormsModule, ReactiveFormsModule } from "@angular/forms"
import  { Router } from "@angular/router"
import { CardModule } from "primeng/card"
import { InputTextModule } from "primeng/inputtext"

import { ButtonModule } from "primeng/button"
import { TableModule } from "primeng/table"
import { ProgressBarModule } from "primeng/progressbar"
import { TagModule } from "primeng/tag"
import { SliderModule } from "primeng/slider"
import { ChipModule } from "primeng/chip"
import  { JobService } from "../../services/job.service"
import  { MatchingService } from "../../services/matching.service"
import  { Candidate } from "../../models/candidate.model"
import  { Job } from "../../models/job.model"

// Update the SkillWeight interface to include a locked property
interface SkillWeight {
  name: string
  weight: number
  previousWeight?: number // To track previous weight for proportional adjustments
  locked: boolean // New property to track if the skill weight is locked
}

// Add this interface after the SkillWeight interface
interface AspectWeights {
  keyResponsibilities: number
  requiredQualifications: number
  preferredSkills: number
  companyOverview: number
}

@Component({
  selector: "app-job-matcher",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    ButtonModule,
    TableModule,
    ProgressBarModule,
    TagModule,
    SliderModule,
    ChipModule,
  ],
  templateUrl: "./job-matcher.component.html",
  styleUrls: ["./job-matcher.component.css"],
})
export class JobMatcherComponent implements OnInit {
  selectedJob: Job | null = null
  matchingCandidates: Candidate[] = []
  skillWeights: SkillWeight[] = []
  loading = false
  loadingCandidates = false
  error: string | null = null
  newSkill = ""
  rows = 10
  totalRecords = 0
  isAdjusting = false // Flag to prevent recursive updates

  // Add these properties to the JobMatcherComponent class
  aspectWeights: AspectWeights = {
    keyResponsibilities: 25,
    requiredQualifications: 25,
    preferredSkills: 25,
    companyOverview: 25,
  }
  isAdjustingAspects = false

  constructor(
    private jobService: JobService,
    private matchingService: MatchingService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.selectedJob = this.jobService.getSelectedJob()

    if (!this.selectedJob) {
      // If no job is selected, redirect back to jobs page
      this.router.navigate(["/jobs"])
      return
    }

    // In the ngOnInit method, update the initialization of skillWeights to include locked: false
    // Initialize skill weights
    if (this.selectedJob.preferredSkills) {
      // Calculate initial equal weights
      const initialWeight =
        this.selectedJob.preferredSkills.length > 0 ? Math.floor(100 / this.selectedJob.preferredSkills.length) : 0

      // Distribute weights evenly
      this.skillWeights = this.selectedJob.preferredSkills.map((skill, index) => {
        // Add any remaining percentage to the first skill
        const weight =
          index === 0
            ? initialWeight + (100 - initialWeight * this.selectedJob!.preferredSkills!.length)
            : initialWeight

        return {
          name: skill,
          weight: weight,
          previousWeight: weight,
          locked: false,
        }
      })
    }

    this.findMatches()
  }

  findMatches() {
    this.loadingCandidates = true
    this.error = null

    // Convert skill weights to the format expected by the API
    const skillWeightsObj = this.skillWeights.reduce(
      (obj, item) => {
        obj[item.name] = item.weight
        return obj
      },
      {} as Record<string, number>,
    )

    // In a real app, you would pass the skill weights to the backend
    this.matchingService.findMatchingCandidates(this.selectedJob!, skillWeightsObj).subscribe({
      next: (candidates) => {
        if (candidates && Array.isArray(candidates)) {
          this.matchingCandidates = candidates
          this.totalRecords = candidates.length
        } else {
          console.error("Invalid candidates data format:", candidates)
          this.error = "Received invalid data format from server"
          this.matchingCandidates = []
          this.totalRecords = 0
        }
        this.loadingCandidates = false
      },
      error: (err) => {
        console.error("Error finding matches:", err)
        this.error = "Failed to find matching candidates. Please try again."
        this.loadingCandidates = false
        this.matchingCandidates = []
        this.totalRecords = 0
      },
    })
  }

  // Add a method to toggle the locked state of a skill
  toggleLockSkill(index: number): void {
    this.skillWeights[index].locked = !this.skillWeights[index].locked
  }

  // Update the updateSkillWeight method to respect locked skills
  updateSkillWeight(updatedSkill: SkillWeight, index: number) {
    // Prevent recursive updates
    if (this.isAdjusting) {
      return
    }

    this.isAdjusting = true

    try {
      // Store the change in weight
      const weightChange = updatedSkill.weight - (updatedSkill.previousWeight || 0)

      // If there's no change or only one skill, no need to adjust
      if (weightChange === 0 || this.skillWeights.length <= 1) {
        updatedSkill.previousWeight = updatedSkill.weight
        return
      }

      // Get all unlocked skills except the one being updated
      const unlockableSkills = this.skillWeights.filter((skill, i) => i !== index && !skill.locked)

      // If there are no unlockable skills, check if adjustment is possible
      if (unlockableSkills.length === 0) {
        // If all other skills are locked, we can't adjust
        const totalLockedWeight = this.skillWeights
          .filter((skill, i) => i !== index)
          .reduce((sum, skill) => sum + skill.weight, 0)

        // Check if the adjustment would make the total exceed 100%
        if (totalLockedWeight + updatedSkill.weight !== 100) {
          // Reset to previous state since we can't adjust
          updatedSkill.weight = updatedSkill.previousWeight || 0

          // Instead of using alert, we'll just log to console and return
          console.warn("Cannot adjust weight. All other skills are locked and total must be 100%.")
          return
        }
      }

      // Calculate the total weight of unlocked skills
      const unlockableSkillsWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0)

      // If unlockable skills have no weight, we can't redistribute
      if (unlockableSkillsWeight <= 0 && weightChange !== 0) {
        // Reset to previous state
        updatedSkill.weight = updatedSkill.previousWeight || 0
        return
      }

      // Adjust unlocked skills proportionally
      if (unlockableSkills.length > 0) {
        unlockableSkills.forEach((skill) => {
          // Calculate the proportion of this skill's weight to the total of unlocked skills
          const proportion = skill.weight / unlockableSkillsWeight

          // Adjust this skill's weight by the opposite of the change, proportionally
          skill.weight = Math.max(0, Math.min(100, skill.weight - weightChange * proportion))

          // Round to avoid floating point issues
          skill.weight = Math.round(skill.weight)

          // Update previous weight
          skill.previousWeight = skill.weight
        })
      }

      // Ensure the total is exactly 100% by adjusting an unlocked skill if needed
      const totalWeight = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)

      if (totalWeight !== 100) {
        // Find an unlocked skill other than the one being adjusted to make the correction
        const adjustableSkills = this.skillWeights.filter((skill, i) => i !== index && !skill.locked)

        if (adjustableSkills.length > 0) {
          // Adjust the first unlocked skill
          adjustableSkills[0].weight += 100 - totalWeight
          adjustableSkills[0].previousWeight = adjustableSkills[0].weight
        } else if (!updatedSkill.locked) {
          // If there are no other unlocked skills, adjust the current one if it's not locked
          updatedSkill.weight += 100 - totalWeight
        } else {
          // If all skills are locked, we can't adjust to 100%
          // Instead of using alert, we'll just reset the values silently
          console.warn("Cannot adjust weights to total 100%. All skills are locked.")

          // Reset to previous values
          this.skillWeights.forEach((skill) => {
            skill.weight = skill.previousWeight || 0
          })
          return
        }
      }

      // Update the previous weight of the changed skill
      updatedSkill.previousWeight = updatedSkill.weight

      console.log(`Updated weights to sum to ${this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)}%`)
    } finally {
      this.isAdjusting = false
    }
  }

  getSkillLevelClass(weight: number): string {
    if (weight >= 75) return "skill-high"
    if (weight >= 50) return "skill-medium"
    if (weight >= 25) return "skill-medium-low"
    return "skill-low"
  }

  // Update the addSkill method to include locked: false
  addSkill() {
    if (this.newSkill.trim() && !this.skillWeights.some((s) => s.name === this.newSkill.trim())) {
      // Calculate how much weight to take from each existing skill
      const newSkillWeight = this.skillWeights.length > 0 ? Math.floor(100 / (this.skillWeights.length + 1)) : 100

      // Get unlocked skills
      const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked)

      if (unlockableSkills.length > 0) {
        // Calculate weight reduction only for unlocked skills
        const weightReduction = Math.floor(newSkillWeight / unlockableSkills.length)

        // Reduce weight of existing unlocked skills
        unlockableSkills.forEach((skill) => {
          skill.weight = Math.max(0, skill.weight - weightReduction)
          skill.previousWeight = skill.weight
        })
      } else {
        // If all skills are locked, log a warning and return
        if (this.skillWeights.length > 0) {
          console.warn("Cannot add new skill. All existing skills are locked.")
          return
        }
      }

      // Add the new skill
      this.skillWeights.push({
        name: this.newSkill.trim(),
        weight: newSkillWeight,
        previousWeight: newSkillWeight,
        locked: false,
      })

      // Ensure total is exactly 100%
      this.normalizeWeights()

      // Update the job's preferred skills
      if (this.selectedJob) {
        this.selectedJob.preferredSkills = this.skillWeights.map((s) => s.name)
      }

      this.newSkill = ""
    }
  }

  // Update the removeSkill method to handle locked skills
  removeSkill(index: number) {
    // Check if the skill is locked
    if (this.skillWeights[index].locked) {
      // Use a more subtle notification or just log to console
      console.warn("Cannot remove a locked skill. Please unlock it first.")
      return
    }

    if (this.skillWeights.length <= 1) {
      // If this is the last skill, just reset it
      this.skillWeights = []

      // Update the job's preferred skills
      if (this.selectedJob) {
        this.selectedJob.preferredSkills = []
      }
      return
    }

    // Get the weight of the skill being removed
    const removedWeight = this.skillWeights[index].weight

    // Remove the skill
    this.skillWeights.splice(index, 1)

    // Get unlocked skills for redistribution
    const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked)

    if (unlockableSkills.length > 0) {
      // Calculate total weight of unlocked skills
      const totalUnlockedWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0)

      // Redistribute the removed weight proportionally among unlocked skills
      unlockableSkills.forEach((skill) => {
        // Calculate proportion of this skill to the total unlocked weight
        const proportion = skill.weight / totalUnlockedWeight

        // Add proportional share of the removed weight
        skill.weight += Math.round(removedWeight * proportion)
        skill.previousWeight = skill.weight
      })
    } else {
      // If all remaining skills are locked, we can't redistribute
      console.warn("Cannot redistribute weight. All remaining skills are locked.")
    }

    // Ensure total is exactly 100%
    this.normalizeWeights()

    // Update the job's preferred skills
    if (this.selectedJob) {
      this.selectedJob.preferredSkills = this.skillWeights.map((s) => s.name)
    }
  }

  // Update the normalizeWeights method to respect locked skills
  normalizeWeights() {
    if (this.skillWeights.length === 0) return

    const totalWeight = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)

    if (totalWeight !== 100) {
      // Find an unlocked skill to adjust
      const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked)

      if (unlockableSkills.length > 0) {
        // Adjust the first unlocked skill
        unlockableSkills[0].weight += 100 - totalWeight
        unlockableSkills[0].previousWeight = unlockableSkills[0].weight
      } else if (this.skillWeights.length > 0) {
        // If all skills are locked, log a warning
        console.warn("Cannot normalize weights to 100%. All skills are locked.")
      }
    }
  }

  // Add a method to count locked skills
  getLockedSkillsCount(): number {
    return this.skillWeights.filter((skill) => skill.locked).length
  }

  // Add a method to check if all skills are locked
  areAllSkillsLocked(): boolean {
    return this.skillWeights.length > 0 && this.skillWeights.every((skill) => skill.locked)
  }

  applyWeights() {
    this.findMatches()
  }

  goBack() {
    this.router.navigate(["/jobs"])
  }

  onRowsChange() {
    // Handle rows per page change
    console.log("Rows changed to:", this.rows)
  }

  // Calculate the total weight of all skills
  getTotalWeight(): number {
    return this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)
  }

  // Add these methods to the JobMatcherComponent class

  // Calculate the total weight of all aspects
  getAspectsWeightTotal(): number {
    return (
      this.aspectWeights.keyResponsibilities +
      this.aspectWeights.requiredQualifications +
      this.aspectWeights.preferredSkills +
      this.aspectWeights.companyOverview
    )
  }

  // Update aspect weights while maintaining 100% total
  updateAspectWeight(changedAspect: keyof AspectWeights) {
    // Prevent recursive updates
    if (this.isAdjustingAspects) {
      return
    }

    this.isAdjustingAspects = true

    try {
      const totalWeight = this.getAspectsWeightTotal()

      // If we already have 100%, no need to adjust
      if (totalWeight === 100) {
        return
      }

      // Get all aspects except the one being changed
      const otherAspects: (keyof AspectWeights)[] = [
        "keyResponsibilities",
        "requiredQualifications",
        "preferredSkills",
        "companyOverview",
      ].filter((aspect) => aspect !== changedAspect)

      // Calculate the total weight of other aspects
      const otherAspectsWeight = otherAspects.reduce((sum, aspect) => sum + this.aspectWeights[aspect], 0)

      // If other aspects have no weight, we can't redistribute
      if (otherAspectsWeight <= 0) {
        // Reset to ensure total is 100%
        this.aspectWeights[changedAspect] = 100
        otherAspects.forEach((aspect) => {
          this.aspectWeights[aspect] = 0
        })
        return
      }

      // Calculate how much we need to adjust (positive if we need to reduce, negative if we need to increase)
      const adjustment = totalWeight - 100

      // Adjust other aspects proportionally
      otherAspects.forEach((aspect) => {
        // Calculate the proportion of this aspect's weight to the total of other aspects
        const proportion = this.aspectWeights[aspect] / otherAspectsWeight

        // Adjust this aspect's weight proportionally
        this.aspectWeights[aspect] = Math.max(0, Math.round(this.aspectWeights[aspect] - adjustment * proportion))
      })

      // Ensure the total is exactly 100% by adjusting one aspect if needed
      const newTotal = this.getAspectsWeightTotal()
      if (newTotal !== 100) {
        // Find the first non-zero aspect to adjust
        for (const aspect of otherAspects) {
          if (this.aspectWeights[aspect] > 0) {
            this.aspectWeights[aspect] += 100 - newTotal
            break
          }
        }

        // If all other aspects are zero, adjust the changed aspect
        if (this.getAspectsWeightTotal() !== 100) {
          this.aspectWeights[changedAspect] += 100 - this.getAspectsWeightTotal()
        }
      }

      console.log(`Updated aspect weights to sum to ${this.getAspectsWeightTotal()}%`)
    } finally {
      this.isAdjustingAspects = false
    }
  }
}
