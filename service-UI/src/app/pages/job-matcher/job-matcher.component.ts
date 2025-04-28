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
  skillWeights: SkillWeight[] = [] // Aceasta este lista pentru ponderi - începe goală
  desiredSkillsFromBackend: string[] = []; // Noua proprietate pentru a afișa skill-urile din backend
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

    // Inițializați lista "Desired Skills" cu cele din backend
    if (this.selectedJob.preferredSkills) {
      this.desiredSkillsFromBackend = [...this.selectedJob.preferredSkills]; // Folosiți spread pentru a crea o copie
    } else {
      this.desiredSkillsFromBackend = [];
    }

    // skillWeights rămâne goală inițial și va fi populată de utilizator prin addSkill()
    this.skillWeights = [];


    // Puteți apela findMatches aici dacă doriți un matching inițial
    // (care va folosi ponderi zero sau logica implicită a backend-ului dacă skillWeights este goală)
    this.findMatches()
  }

  findMatches() {
    this.loadingCandidates = true
    this.error = null

    // Convert skill weights (DOAR cele adăugate de utilizator) to the format expected by the API
    const skillWeightsObj = this.skillWeights.reduce(
      (obj, item) => {
        obj[item.name] = item.weight
        return obj
      },
      {} as Record<string, number>,
    )

    // In a real app, you would pass the skill weights to the backend
    // Backend-ul trebuie să gestioneze cazul în care skillWeightsObj este gol
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
  // Aceasta metodă manipulează DOAR skillWeights (cele adăugate de utilizator)
  updateSkillWeight(updatedSkill: SkillWeight, index: number) {
    // Prevent recursive updates
    if (this.isAdjusting) {
      return
    }

    this.isAdjusting = true

    try {
      // Store the change in weight
      const weightChange = updatedSkill.weight - (updatedSkill.previousWeight || 0)

      // If there's no change or only one skill (among user-added), no need to adjust
      if (weightChange === 0 || this.skillWeights.length <= 1) {
        updatedSkill.previousWeight = updatedSkill.weight
        return
      }

      // Get all unlocked skills (among user-added) except the one being updated
      const unlockableSkills = this.skillWeights.filter((skill, i) => i !== index && !skill.locked)

      // If there are no unlockable skills, check if adjustment is possible
      if (unlockableSkills.length === 0) {
        // If all other user-added skills are locked, we can't adjust
        const totalLockedWeight = this.skillWeights
          .filter((skill, i) => i !== index)
          .reduce((sum, skill) => sum + skill.weight, 0)

        // Check if the adjustment would make the total exceed 100%
        if (totalLockedWeight + updatedSkill.weight !== 100) {
          // Reset to previous state since we can't adjust
          updatedSkill.weight = updatedSkill.previousWeight || 0

          console.warn("Cannot adjust weight. All other skills are locked and total must be 100%.")
          return
        }
      }

      // Calculate the total weight of unlocked skills (among user-added)
      const unlockableSkillsWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0)

      // If unlockable skills have no weight, we can't redistribute unless the current skill
      // is the only one with weight among unlockable ones and total is not 100
      if (unlockableSkillsWeight <= 0 && weightChange !== 0) {
        const totalOtherUserAddedWeight = this.skillWeights.reduce((sum, skill, i) => i !== index ? sum + skill.weight : sum, 0);
        // If the current skill is the only one with weight among user-added skills, and total != 100, it can be adjusted to 100
        if (totalOtherUserAddedWeight === 0 && this.skillWeights[index].weight !== 100 && this.skillWeights.length > 0) {
          this.skillWeights[index].weight = 100;
          this.skillWeights.forEach((skill, i) => {
            if(i !== index) skill.weight = 0;
          });
          console.log("Adjusted the single user-added skill to 100% as others had 0 weight.");
          // Need to update previous weights after this forced adjustment
          this.skillWeights.forEach(skill => skill.previousWeight = skill.weight);
          return; // Adjustment done
        } else {
          // Otherwise, if no unlockable weight is available, we can't make the change
          updatedSkill.weight = updatedSkill.previousWeight || 0;
          console.warn("Cannot adjust weight. No unlockable weight available for redistribution.");
          return;
        }
      }


      // Adjust unlocked skills proportionally (among user-added)
      if (unlockableSkills.length > 0) {
        unlockableSkills.forEach((skill) => {
          // Calculate the proportion of this skill's weight to the total of unlocked skills
          const proportion = skill.weight / unlockableSkillsWeight;

          // Calculate the proportional change needed for this skill
          const proportionalChange = -weightChange * proportion;

          // Apply the change, ensuring weight stays between 0 and 100
          skill.weight = Math.max(0, Math.min(100, skill.weight + proportionalChange));

          // Round to avoid floating point issues
          skill.weight = Math.round(skill.weight);

          // previousWeight will be updated at the end
        });
      }


      // Ensure the total of user-added skills is exactly 100% by adjusting an unlocked skill if needed
      const totalWeight = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)

      if (totalWeight !== 100) {
        const difference = 100 - totalWeight; // How much we need to add or subtract

        // Find an unlocked skill (among user-added) to make the correction
        const unlockableSkillsForCorrection = this.skillWeights.filter((skill) => !skill.locked);

        if (unlockableSkillsForCorrection.length > 0) {
          // Prioritize adjusting a skill other than the one being changed if possible,
          // and preferably one with weight > 0.
          const otherUnlockedSkills = unlockableSkillsForCorrection.filter(skill => skill.name !== updatedSkill.name && skill.weight > 0);
          const anyOtherUnlockedSkills = unlockableSkillsForCorrection.filter(skill => skill.name !== updatedSkill.name);


          if (otherUnlockedSkills.length > 0) {
            // Adjust the first other unlocked skill with weight > 0
            otherUnlockedSkills[0].weight += difference;
            // Ensure it stays non-negative and round
            otherUnlockedSkills[0].weight = Math.max(0, Math.round(otherUnlockedSkills[0].weight));
          } else if (anyOtherUnlockedSkills.length > 0) {
            // If no other unlocked skill has weight > 0, adjust the first one found
            anyOtherUnlockedSkills[0].weight += difference;
            anyOtherUnlockedSkills[0].weight = Math.max(0, Math.round(anyOtherUnlockedSkills[0].weight));
          }
          else if (!updatedSkill.locked) {
            // If the only unlocked skill is the one being changed, adjust it (if it's not locked)
            updatedSkill.weight += difference;
            updatedSkill.weight = Math.max(0, Math.round(updatedSkill.weight));
          } else {
            // This case should ideally not be reached if unlockableSkillsForCorrection > 0
            console.warn("Logic error in weight adjustment correction.");
          }

        } else if (this.skillWeights.length > 0) {
          // If all user-added skills are locked and total is not 100, log a warning
          console.warn("Cannot adjust weights to total 100%. All user-added skills are locked.")
          // Reset to previous values as adjustment was not possible
          this.skillWeights.forEach(skill => {
            skill.weight = skill.previousWeight || 0;
          });
          return; // Stop execution if all are locked
        } else {
          // If skillWeights is empty, this case shouldn't be reached with weight != 100, but good to handle
          console.warn("Skill weights array is empty during normalization.");
          return;
        }
      }

      // Ensure all weights are non-negative and round after all adjustments
      this.skillWeights.forEach(skill => {
        skill.weight = Math.max(0, Math.round(skill.weight));
      });

      // One final pass to ensure total is exactly 100 after rounding
      const finalTotal = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0);
      if (finalTotal !== 100 && this.skillWeights.length > 0) {
        const finalDifference = 100 - finalTotal;
        const unlockableForFinal = this.skillWeights.filter(skill => !skill.locked);
        if(unlockableForFinal.length > 0) {
          // Add the tiny difference to the first unlocked skill
          unlockableForFinal[0].weight += finalDifference;
          unlockableForFinal[0].weight = Math.max(0, unlockableForFinal[0].weight);
        } else {
          console.warn("Cannot make final micro-adjustment. All user-added skills are locked.");
        }
      }


      // Update the previous weight of all skills after all adjustments
      this.skillWeights.forEach(skill => {
        skill.previousWeight = skill.weight;
      });


      console.log(`Updated user-added skill weights to sum to ${this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)}%`)
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

  // Update the addSkill method to handle adding the first skill and not update selectedJob.preferredSkills
  addSkill() {
    const skillName = this.newSkill.trim();
    // Prevent adding empty or duplicate skills
    if (skillName && !this.skillWeights.some((s) => s.name.toLowerCase() === skillName.toLowerCase())) {

      const numberOfSkills = this.skillWeights.length;
      let newSkillWeight = 0;

      if (numberOfSkills === 0) {
        // If adding the first skill, it gets 100%
        newSkillWeight = 100;
        this.skillWeights.push({
          name: skillName,
          weight: newSkillWeight,
          previousWeight: newSkillWeight, // previousWeight is same initially
          locked: false,
        });
      } else {
        // If adding to existing skills
        const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked);
        const totalUnlockedWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0);

        if (unlockableSkills.length === 0) {
          // If all existing skills are locked, we cannot add a new skill with weight > 0
          console.warn("Cannot add new skill. All existing skills for matching are locked and no weight can be taken.");
          this.newSkill = ""; // Clear input even if not added
          return;
        }

        // Strategy: Take a fixed percentage from the total available unlocked weight for the new skill,
        // and redistribute the rest proportionally among unlocked skills.
        // Let's aim for the new skill to take 100 / (numberOfSkills + 1) if possible from unlocked weight.
        const idealNewSkillWeight = 100 / (numberOfSkills + 1);
        let weightToTakeForNewSkill = idealNewSkillWeight;


        if (totalUnlockedWeight < weightToTakeForNewSkill && totalUnlockedWeight > 0) {
          // If not enough unlocked weight for the ideal, take all available unlocked weight
          weightToTakeForNewSkill = totalUnlockedWeight;
          console.warn(`Not enough unlocked weight for ideal distribution. New skill gets ${weightToTakeForNewSkill}%`);

        } else if (totalUnlockedWeight <= 0) {
          // Should be caught by unlockableSkills.length === 0 check, but double check
          console.warn("Cannot add new skill. No unlocked weight available.");
          this.newSkill = "";
          return;
        }

        newSkillWeight = weightToTakeForNewSkill;
        const weightToRedistribute = weightToTakeForNewSkill; // This is the amount we need to take from others


        // Reduce weight of existing unlocked skills proportionally
        unlockableSkills.forEach((skill) => {
          // Calculate the proportion of this unlocked skill's weight to the total unlocked weight
          // Avoid division by zero if totalUnlockedWeight is 0 (should be handled above)
          const proportionOfUnlocked = totalUnlockedWeight > 0 ? skill.weight / totalUnlockedWeight : 0;

          // Reduce this skill's weight proportionally to the weight being taken for the new skill
          skill.weight = Math.max(0, skill.weight - (weightToRedistribute * proportionOfUnlocked));

          // Round to avoid floating point issues
          skill.weight = Math.round(skill.weight);

          // previousWeight will be updated during normalization
        });

        // Add the new skill
        this.skillWeights.push({
          name: skillName,
          weight: newSkillWeight,
          previousWeight: newSkillWeight, // previousWeight is same initially
          locked: false,
        });

        // Ensure total of user-added skills is exactly 100% after adding and adjusting
        this.normalizeWeights();
      }


      // *** NU MAI ACTUALIZĂM selectedJob.preferredSkills AICI ***
      // Această listă rămâne cea venită din backend

      this.newSkill = ""; // Clear the input field
    }
  }

  // Update the removeSkill method to handle locked skills and not update selectedJob.preferredSkills
  removeSkill(index: number) {
    // Check if the skill is locked
    if (this.skillWeights.length > index && this.skillWeights[index].locked) {
      console.warn("Cannot remove a locked skill. Please unlock it first.");
      return;
    }

    // Store the weight of the skill being removed
    const removedWeight = this.skillWeights.length > index ? this.skillWeights[index].weight : 0;

    // Remove the skill from the skillWeights array
    if (this.skillWeights.length > index) {
      this.skillWeights.splice(index, 1);
    } else {
      console.warn("Attempted to remove skill at invalid index.");
      return;
    }


    // If there are no skills left, we are done
    if (this.skillWeights.length === 0) {
      // *** NU MAI ACTUALIZĂM selectedJob.preferredSkills AICI ***
      return;
    }


    // Get unlocked skills for redistribution among the *remaining* user-added skills
    const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked);

    if (unlockableSkills.length > 0) {
      // Calculate total weight of the *remaining* unlocked skills
      const totalRemainingUnlockedWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0);

      // Redistribute the removed weight proportionally among remaining unlocked skills
      unlockableSkills.forEach((skill) => {
        // Calculate proportion of this skill to the total remaining unlocked weight
        // Avoid division by zero if somehow totalRemainingUnlockedWeight is 0 (shouldn't happen if unlockableSkills.length > 0)
        const proportion = totalRemainingUnlockedWeight > 0 ? skill.weight / totalRemainingUnlockedWeight : 0;

        // Add proportional share of the removed weight
        skill.weight += Math.round(removedWeight * proportion);
        // previousWeight will be updated during normalization
      });
    } else {
      // If all remaining user-added skills are locked, we can't redistribute
      console.warn("Cannot redistribute removed weight. All remaining user-added skills are locked.");
      // In this case, the total weight will drop below 100 for the user-added skills.
      // You might want to handle this scenario explicitly if the total must always be 100,
      // perhaps by resetting weights or preventing removal if all remaining are locked.
    }

    // Ensure total of user-added skills is exactly 100% after removal and redistribution
    this.normalizeWeights();

    // *** NU MAI ACTUALIZĂM selectedJob.preferredSkills AICI ***
    // Această listă rămâne cea venită din backend
  }

  // Update the normalizeWeights method to respect locked skills and handle empty array
  // Aceasta manipulează DOAR skillWeights
  normalizeWeights() {
    if (this.skillWeights.length === 0) return; // Nothing to normalize if empty

    const totalWeight = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0);

    if (totalWeight !== 100) {
      const difference = 100 - totalWeight; // How much we need to add or subtract

      // Find an unlocked skill (among user-added) to adjust
      const unlockableSkills = this.skillWeights.filter((skill) => !skill.locked);

      if (unlockableSkills.length > 0) {
        // Attempt to distribute the difference proportionally based on current weights
        const totalUnlockableWeight = unlockableSkills.reduce((sum, skill) => sum + skill.weight, 0);

        if (totalUnlockableWeight > 0) {
          unlockableSkills.forEach(skill => {
            const proportion = skill.weight / totalUnlockableWeight;
            skill.weight += Math.round(difference * proportion);
          });

          // Final adjustment for potential rounding errors - add/subtract from the first unlocked skill
          const currentTotal = this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0);
          const finalDifference = 100 - currentTotal;
          if (finalDifference !== 0) {
            unlockableSkills[0].weight += finalDifference;
          }

        } else {
          // If totalUnlockableWeight is 0 but there are unlockable skills (meaning they all have 0 weight)
          // Add the entire difference to the first unlocked skill
          unlockableSkills[0].weight += difference;
        }

        // Ensure all weights are non-negative after adjustment
        this.skillWeights.forEach(skill => {
          skill.weight = Math.max(0, Math.round(skill.weight));
        });

      } else if (this.skillWeights.length > 0) {
        // If all user-added skills are locked and total is not 100, log a warning
        console.warn("Cannot normalize user-added skill weights to 100%. All skills are locked and total is not 100.");
        // You might want to reset weights to previous here if this happens often
        // this.skillWeights.forEach(skill => skill.weight = skill.previousWeight || 0);
      }
      // If skillWeights is empty, the initial check returns.
    }

    // Update previousWeight for all user-added skills after normalization
    this.skillWeights.forEach(skill => {
      skill.previousWeight = skill.weight;
    });
  }


  // Add a method to count locked skills (among user-added)
  getLockedSkillsCount(): number {
    return this.skillWeights.filter((skill) => skill.locked).length
  }

  // Add a method to check if all user-added skills are locked
  areAllSkillsLocked(): boolean {
    return this.skillWeights.length > 0 && this.skillWeights.every((skill) => skill.locked)
  }

  applyWeights() {
    // Aceasta metodă folosește DOAR skillWeights (cele adăugate de utilizator)
    this.findMatches()
  }

  goBack() {
    this.router.navigate(["/jobs"])
  }

  onRowsChange() {
    // Handle rows per page change
    console.log("Rows changed to:", this.rows)
  }

  // Calculate the total weight of all user-added skills
  getTotalWeight(): number {
    return this.skillWeights.reduce((sum, skill) => sum + skill.weight, 0)
  }

  // These methods handle aspect weights, which is separate from skill weights
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
      const otherAspects: (keyof AspectWeights)[] = (
        [
          "keyResponsibilities",
          "requiredQualifications",
          "preferredSkills",
          "companyOverview",
        ] as (keyof AspectWeights)[]
      ).filter((aspect) => aspect !== changedAspect)

      // Calculate the total weight of other aspects
      const otherAspectsWeight = otherAspects.reduce((sum, aspect) => sum + this.aspectWeights[aspect], 0)

      // If other aspects have no weight, we can't redistribute unless the changed aspect
      // is the only one with weight and the total isn't 100.
      if (otherAspectsWeight <= 0) {
        // If the changed aspect's weight is the only non-zero, and total isn't 100
        if (this.aspectWeights[changedAspect] !== 100 && totalWeight > 0) {
          // Set the changed aspect to 100 and others to 0
          this.aspectWeights[changedAspect] = 100;
          otherAspects.forEach((aspect) => {
            this.aspectWeights[aspect] = 0;
          });
          console.log(`Adjusted to ensure changed aspect is 100% as others had no weight.`);
        } else if (totalWeight === 0 && otherAspects.length > 0) {
          // If everything is zero and we are changing one, set that one to 100
          this.aspectWeights[changedAspect] = 100;
          otherAspects.forEach((aspect) => {
            this.aspectWeights[aspect] = 0;
          });
          console.log(`Adjusted to set changed aspect to 100% from zero total.`);
        } else {
          // If totalWeight is already 100 (handled above) or if all are zero and we are changing one to zero,
          // or other edge cases where redistribution isn't possible/needed.
          console.warn("Cannot redistribute weight. Other aspects have zero or negative weight.");
          // You might want to reset the changedAspect to its previous value here if you track it.
        }
        return;
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
      let newTotal = this.getAspectsWeightTotal();
      if (newTotal !== 100) {
        const difference = 100 - newTotal;
        // Find an aspect to adjust - prioritize one that wasn't the one initially changed if possible,
        // and preferably one with weight > 0.
        const adjustableAspects = otherAspects.filter(aspect => this.aspectWeights[aspect] >= 0); // Can adjust any non-negative

        if (adjustableAspects.length > 0) {
          // Adjust the first available adjustable aspect
          // Use the aspect key to access the property in this.aspectWeights
          this.aspectWeights[adjustableAspects[0]] = Math.max(0, this.aspectWeights[adjustableAspects[0]] + difference); // Ensure non-negative
        } else {
          // If for some reason other aspects aren't adjustable (shouldn't happen with >= 0),
          // or if otherAspects is empty (meaning the changedAspect is the only one), adjust the changed aspect.
          this.aspectWeights[changedAspect] += difference;
          this.aspectWeights[changedAspect] = Math.max(0, this.aspectWeights[changedAspect]); // Ensure non-negative
        }

      }

      // Final check to ensure total is 100 after adjustments, accounting for potential rounding issues
      const finalTotal = this.getAspectsWeightTotal();
      if (finalTotal !== 100) {
        const finalDifference = 100 - finalTotal;
        // Find an aspect to make the final micro-adjustment - can be any aspect
        const allAspectKeys: (keyof AspectWeights)[] = ["keyResponsibilities", "requiredQualifications", "preferredSkills", "companyOverview"];
        if (allAspectKeys.length > 0) {
          // Use the aspect key to access the property in this.aspectWeights
          this.aspectWeights[allAspectKeys[0]] += finalDifference;
          this.aspectWeights[allAspectKeys[0]] = Math.max(0, this.aspectWeights[allAspectKeys[0]]); // Ensure non-negative
        }
      }


      console.log(`Updated aspect weights to sum to ${this.getAspectsWeightTotal()}%`)
    } finally {
      this.isAdjustingAspects = false
    }
  }
}
