import { Component, type OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { FormsModule } from "@angular/forms"
import  { Router } from "@angular/router"
import  { Job } from "../../models/job.model"
import  { JobService } from "../../services/job.service"

interface JobWithSelection extends Job {
  selected?: boolean
}

@Component({
  selector: "app-jobs",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./jobs.component.html",
  styleUrls: ["./jobs.component.css"],
})
export class JobsComponent implements OnInit {
  jobs: JobWithSelection[] = []
  rows = 10
  totalRecords = 0
  allSelected = false
  uploadedFiles: File[] = []
  isDragOver = false

  constructor(
    private jobService: JobService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.loadJobs()
  }

  loadJobs() {
    this.jobService.getJobs().subscribe((data) => {
      this.jobs = data.map((job) => ({ ...job, selected: false }))
      this.totalRecords = data.length
    })
  }

  // File upload methods
  onDragOver(event: DragEvent) {
    event.preventDefault()
    event.stopPropagation()
    this.isDragOver = true
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault()
    event.stopPropagation()
    this.isDragOver = false
  }
  // Navigate to job-matcher with the selected job
  viewJobMatches(job: Job) {
    this.jobService.setSelectedJob(job)
    this.router.navigate(["/job-matcher"])
  }

  onDrop(event: DragEvent) {
    event.preventDefault()
    event.stopPropagation()
    this.isDragOver = false

    if (event.dataTransfer?.files) {
      this.handleFiles(event.dataTransfer.files)
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement
    if (input.files) {
      this.handleFiles(input.files)
    }
  }

  handleFiles(files: FileList) {
    Array.from(files).forEach((file) => {
      // Check if file is PDF, DOC, or DOCX
      if (
        file.type === "application/pdf" ||
        file.type === "application/msword" ||
        file.type === "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      ) {
        this.uploadedFiles.push(file)
      }
    })
  }

  removeFile(index: number) {
    this.uploadedFiles.splice(index, 1)
  }

  clearFiles() {
    this.uploadedFiles = []
  }

  processFiles() {
    // In a real application, you would send these files to your backend
    console.log("Processing files:", this.uploadedFiles)

    // Show success message or handle the response
    alert(`Successfully processed ${this.uploadedFiles.length} files`)

    // Clear the files after processing
    this.clearFiles()
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 Bytes"

    const k = 1024
    const sizes = ["Bytes", "KB", "MB", "GB"]
    const i = Math.floor(Math.log(bytes) / Math.log(k))

    return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i]
  }

  getFileIcon(file: File): string {
    if (file.type === "application/pdf") {
      return "pi-file-pdf"
    } else if (
      file.type === "application/msword" ||
      file.type === "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ) {
      return "pi-file-word"
    }
    return "pi-file"
  }

  // Job selection methods
  toggleJobSelection(index: number) {
    this.jobs[index].selected = !this.jobs[index].selected
    this.updateSelectAllState()
  }

  toggleSelectAll() {
    this.allSelected = !this.allSelected
    this.jobs.forEach((job) => (job.selected = this.allSelected))
  }

  updateSelectAllState() {
    this.allSelected = this.jobs.length > 0 && this.jobs.every((job) => job.selected)
  }

  hasSelectedJobs(): boolean {
    return this.jobs.some((job) => job.selected)
  }

  deleteSelectedJobs() {
    if (confirm("Are you sure you want to delete the selected jobs?")) {
      // In a real application, you would call your service to delete these jobs
      this.jobs = this.jobs.filter((job) => !job.selected)
      this.totalRecords = this.jobs.length

      // Show success message
      alert("Selected jobs have been deleted")
    }
  }

  getStatusDotClass(job: Job): string {
    // This is a placeholder - in a real app, you'd have logic to determine if a job is active
    return "active"
  }

  getDepartment(job: Job): string {
    // Extract department from job data
    // This is a placeholder - in a real app, you'd have this data in your job model
    if (
      job.positionName.includes("Developer") ||
      job.positionName.includes("Officer") ||
      job.positionName.includes("Engineer")
    ) {
      return "Technology"
    } else if (job.positionName.includes("Marketing")) {
      return "Marketing"
    } else if (job.positionName.includes("Sales")) {
      return "Sales"
    } else {
      return "General"
    }
  }

  getDepartmentClass(job: Job): string {
    const department = this.getDepartment(job)
    switch (department) {
      case "Technology":
        return "department-tech"
      case "Marketing":
        return "department-marketing"
      case "Sales":
        return "department-sales"
      default:
        return "department-finance"
    }
  }

  getDepartmentIcon(job: Job): string {
    const department = this.getDepartment(job)
    switch (department) {
      case "Technology":
        return "pi-desktop"
      case "Marketing":
        return "pi-chart-line"
      case "Sales":
        return "pi-dollar"
      default:
        return "pi-briefcase"
    }
  }

  getJobStageBadgeClass(stage: string): string {
    if (!stage) return ""

    if (stage.toLowerCase().includes("probation")) {
      return "badge-probation"
    } else if (stage.toLowerCase().includes("interview")) {
      return "badge-interview"
    } else if (stage.toLowerCase().includes("start")) {
      return "badge-started"
    } else if (stage.toLowerCase().includes("offer")) {
      return "badge-offered"
    }
    return ""
  }

  getMaxSalary(job: Job): string {
    // This is a placeholder - in a real app, you'd have this data in your job model
    if (job.salary && job.salary.includes("USD")) {
      const minSalary = Number.parseInt(job.salary.replace(/[^0-9]/g, ""))
      return `${minSalary + 10000} USD`
    }
    return "Negotiable"
  }

  onRowsChange() {
    // Handle rows per page change
    console.log("Rows changed to:", this.rows)
  }

  navigateToJobMatcher() {
    this.router.navigate(["/job-matcher"])
  }

  navigateToCvMatcher() {
    this.router.navigate(["/cv-matcher"])
  }
}
