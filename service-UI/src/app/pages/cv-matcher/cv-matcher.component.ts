import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { CardModule } from "primeng/card";
import { InputTextModule } from "primeng/inputtext";
import { ButtonModule } from "primeng/button";
import { TableModule } from "primeng/table";
import { ProgressBarModule } from "primeng/progressbar";
import { TagModule } from "primeng/tag";
import { ChipModule } from "primeng/chip";
import { CandidateService } from "../../services/candidate.service";
import { MatchingService } from "../../services/matching.service";
import { Candidate } from "../../models/candidate.model";
import { Job } from "../../models/job.model";
// *** Add Observable import if not already implicitly available ***
import { Observable } from 'rxjs';
import {CrossOrigin} from '@angular-devkit/build-angular';

// Interface for the data structure coming from the backend
interface JobMatchResponseDTO {
  score: number;
  explanation: Record<string, string>;
  jobDescriptionViewDTO: Job;
}

// Interface representing the structure stored in matchingJobs array
interface JobWithScore extends Job {
  score: number; // Add the score property
}

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
  selectedCandidate: Candidate | null = null;
  // *** Use the JobWithScore type ***
  matchingJobs: JobWithScore[] = [];
  loading = false;
  loadingJobs = false;
  error: string | null = null;
  rows = 10;
  totalRecords = 0;

  public Array = Array;

  constructor(
    private candidateService: CandidateService,
    private matchingService: MatchingService,
    private router: Router
  ) {}

  ngOnInit() {
    this.selectedCandidate = this.candidateService.getSelectedCandidate();

    if (!this.selectedCandidate) {
      this.router.navigate(["/candidates"]);
      return;
    }

    this.findMatches();
  }

  findMatches() {
    this.loadingJobs = true;
    this.error = null;

    if (!this.selectedCandidate || !this.selectedCandidate.id) {
      this.error = "Invalid candidate data. Missing candidate ID.";
      this.loadingJobs = false;
      return;
    }

    // *** Cast the observable to the correct DTO type ***
    // Cast first to 'unknown', then to the desired Observable type
    (this.matchingService.findMatchingJobs(this.selectedCandidate.id) as unknown as Observable<JobMatchResponseDTO[]>)
      .subscribe({
        next: (jobMatchResponses: JobMatchResponseDTO[]) => {
          // ... rest of your existing subscribe logic remains the same
          if (jobMatchResponses && Array.isArray(jobMatchResponses)) {
            this.matchingJobs = jobMatchResponses.map((response): JobWithScore => {
              const job = response.jobDescriptionViewDTO;
              return {
                ...job,
                score: response.score,
                requiredQualifications: Array.isArray(job.requiredQualifications)
                  ? job.requiredQualifications
                  : job.requiredQualifications
                    ? [job.requiredQualifications]
                    : []
              };
            });
            this.totalRecords = this.matchingJobs.length;
          } else {
            console.error("Invalid jobs data format:", jobMatchResponses);
            this.error = "Received invalid data format from server";
            this.matchingJobs = [];
            this.totalRecords = 0;
          }
          this.loadingJobs = false;
        },
        error: (err) => {
          console.error("Error finding matches:", err);
          this.error = "Failed to find matching jobs. Please try again.";
          this.loadingJobs = false;
          this.matchingJobs = [];
          this.totalRecords = 0;
        },
      });
  }

  // This method signature is correct as it expects the score number
  getMatchScoreClass(score: number): string {
    if (score >= 80) return "match-score-high";
    if (score >= 60) return "match-score-medium";
    return "match-score-low";
  }

  goBack() {
    this.router.navigate(["/candidates"]);
  }

  onRowsChange() {
    console.log("Rows changed to:", this.rows);
    // Add actual pagination logic here if needed
  }
}
