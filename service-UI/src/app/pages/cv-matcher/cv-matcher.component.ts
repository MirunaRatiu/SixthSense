import { Component } from "@angular/core"
import { CommonModule } from "@angular/common"
import { FormsModule, ReactiveFormsModule, FormBuilder, type FormGroup, Validators } from "@angular/forms"
import { CardModule } from "primeng/card"
import { FileUploadModule } from "primeng/fileupload"
import { ButtonModule } from "primeng/button"
import { TableModule } from "primeng/table"
import { ProgressBarModule } from "primeng/progressbar"
import { TagModule } from "primeng/tag"
import { MatchingService } from "../../services/matching.service"
import type { Job } from "../../models/job.model"
import { Injectable } from "@angular/core"
@Component({
  selector: "app-cv-matcher",
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    CardModule,
    FileUploadModule,
    ButtonModule,
    TableModule,
    ProgressBarModule,
    TagModule,
  ],
  templateUrl: "./cv-matcher.component.html",
  styleUrls: ["./cv-matcher.component.css"],
})
export class CvMatcherComponent {
  cvForm: FormGroup
  matchingJobs: Job[] = []
  cvUploaded = false
  submitted = false

  constructor(
    private fb: FormBuilder,
    private matchingService: MatchingService,
  ) {
    this.cvForm = this.fb.group({
      name: ["", Validators.required],
      skills: [""],
      experience: [0],
    })
  }

  onUpload(event: any) {
    // Handle file upload
    this.cvUploaded = true
  }

  findMatches() {
    if (this.cvForm.invalid || !this.cvUploaded) {
      return
    }

    this.submitted = true
    const candidateData = this.cvForm.value

    this.matchingService.findMatchingJobs(candidateData).subscribe((jobs) => {
      this.matchingJobs = jobs
    })
  }
}
