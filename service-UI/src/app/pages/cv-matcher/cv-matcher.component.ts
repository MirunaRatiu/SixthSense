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
import { ChipModule } from "primeng/chip"
import  { CandidateService } from "../../services/candidate.service"
import  { MatchingService } from "../../services/matching.service"
import  { Candidate } from "../../models/candidate.model"
import  { Job } from "../../models/job.model"

@Component({
  selector: "app-cv-matcher",
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
    ChipModule,
  ],
  templateUrl: "./cv-matcher.component.html",
  styleUrls: ["./cv-matcher.component.css"],
})
export class CvMatcherComponent implements OnInit {
  selectedCandidate: Candidate | null = null
  matchingJobs: Job[] = []
  loading = false
  loadingJobs = false
  error: string | null = null
  rows = 10
  totalRecords = 0

  constructor(
    private candidateService: CandidateService,
    private matchingService: MatchingService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.selectedCandidate = this.candidateService.getSelectedCandidate()

    if (!this.selectedCandidate) {
      // If no candidate is selected, redirect back to candidates page
      this.router.navigate(["/candidates"])
      return
    }

    this.findMatches()
  }

  findMatches() {
    this.loadingJobs = true
    this.error = null

    // In a real app, you would pass the candidate data to the backend
    this.matchingService.findMatchingJobs(this.selectedCandidate!).subscribe({
      next: (jobs) => {
        if (jobs && Array.isArray(jobs)) {
          this.matchingJobs = jobs
          this.totalRecords = jobs.length
        } else {
          console.error("Invalid jobs data format:", jobs)
          this.error = "Received invalid data format from server"
          this.matchingJobs = []
          this.totalRecords = 0
        }
        this.loadingJobs = false
      },
      error: (err) => {
        console.error("Error finding matches:", err)
        this.error = "Failed to find matching jobs. Please try again."
        this.loadingJobs = false
        this.matchingJobs = []
        this.totalRecords = 0
      },
    })
  }

  getMatchScoreClass(score: number): string {
    if (score >= 80) return "match-score-high"
    if (score >= 60) return "match-score-medium"
    return "match-score-low"
  }

  goBack() {
    this.router.navigate(["/candidates"])
  }

  onRowsChange() {
    // Handle rows per page change
    console.log("Rows changed to:", this.rows)
  }
}
