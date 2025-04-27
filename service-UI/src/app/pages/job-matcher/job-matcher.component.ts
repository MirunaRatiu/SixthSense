import { Component, type OnInit } from "@angular/core"
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

interface SkillWeight {
  name: string
  weight: number
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

    // Initialize skill weights
    if (this.selectedJob.preferredSkills) {
      this.skillWeights = this.selectedJob.preferredSkills.map((skill) => ({
        name: skill,
        weight: 50 // Default weight of  => ({
      }))
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
        this.matchingCandidates = candidates
        this.totalRecords = candidates.length
        this.loadingCandidates = false
      },
      error: (err) => {
        console.error("Error finding matches:", err)
        this.error = "Failed to find matching candidates. Please try again."
        this.loadingCandidates = false
      },
    })
  }

  updateSkillWeight(skill: SkillWeight) {
    console.log(`Updated weight for ${skill.name}: ${skill.weight}%`)
  }

  getSkillLevelClass(weight: number): string {
    if (weight >= 75) return "skill-high"
    if (weight >= 50) return "skill-medium"
    return "skill-low"
  }

  addSkill() {
    if (this.newSkill.trim() && !this.skillWeights.some((s) => s.name === this.newSkill.trim())) {
      this.skillWeights.push({
        name: this.newSkill.trim(),
        weight: 50,
      })

      // Update the job's preferred skills
      if (this.selectedJob) {
        this.selectedJob.preferredSkills = this.skillWeights.map((s) => s.name)
      }

      this.newSkill = ""
    }
  }

  removeSkill(index: number) {
    this.skillWeights.splice(index, 1)

    // Update the job's preferred skills
    if (this.selectedJob) {
      this.selectedJob.preferredSkills = this.skillWeights.map((s) => s.name)
    }
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
}
