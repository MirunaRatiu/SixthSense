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
import { ChipModule } from 'primeng/chip';
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
    if (this.selectedJob.requiredSkills) {
      this.skillWeights = this.selectedJob.requiredSkills.map((skill) => ({
        name: skill,
        weight: 50, // Default weight of 50%
      }))
    }

    this.findMatches()
  }

  findMatches() {
    this.loading = true

    // In a real app, you would pass the skill weights to the backend
    this.matchingService.findMatchingCandidates(this.selectedJob).subscribe(
      (candidates) => {
        this.matchingCandidates = candidates
        this.totalRecords = candidates.length
        this.loading = false
      },
      (error) => {
        console.error("Error finding matches:", error)
        this.loading = false
      },
    )
  }

  updateSkillWeight(skill: SkillWeight) {
    console.log(`Updated weight for ${skill.name}: ${skill.weight}%`)
    // In a real app, you would recalculate matches based on new weights
    this.findMatches()
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

      // Update the job's required skills
      if (this.selectedJob) {
        this.selectedJob.requiredSkills = this.skillWeights.map((s) => s.name)
      }

      this.newSkill = ""
      this.findMatches()
    }
  }

  removeSkill(index: number) {
    this.skillWeights.splice(index, 1)

    // Update the job's required skills
    if (this.selectedJob) {
      this.selectedJob.requiredSkills = this.skillWeights.map((s) => s.name)
    }

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
