import { Component,  OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { FormsModule } from "@angular/forms"
import  { Router } from "@angular/router"
import { HttpEventType, HttpResponse } from "@angular/common/http"
import  { Job } from "../../models/job.model"
import  { JobService } from "../../services/job.service"
import  { UploadService } from "../../services/upload.service"

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
  jobs: any[] = []
  rows = 10
  totalRecords = 0
  allSelected = false
  uploadedFiles: File[] = []
  isDragOver = false
  loading = false
  error: string | null = null
  isDeleting = false

  // File upload properties
  currentFile?: File
  progress = 0
  uploadMessage = ""
  isUploading = false
  fileInfos: any[] = []

  constructor(
    private jobService: JobService,
    private uploadService: UploadService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.loadJobs()
  }

  loadJobs() {
    this.loading = true
    this.error = null
    console.log("Starting to load jobs...")

    this.jobService.getJobs().subscribe({
      next: (data) => {
        console.log("Jobs data received:", data)
        // Use the data directly without transformation
        if (data) {
          this.jobs = Array.isArray(data) ? data : [data]
          this.jobs = this.jobs.map((job) => ({ ...job, selected: false }))
          this.totalRecords = this.jobs.length
          console.log("Processed jobs:", this.jobs)
        } else {
          console.error("Invalid data format received:", data)
          this.error = "Received invalid data format from server"
          this.jobs = []
          this.totalRecords = 0
        }
        this.loading = false
      },
      error: (err) => {
        this.error = "Failed to load jobs. Please try again."
        this.loading = false
        console.error("Error loading jobs:", err)
        this.jobs = []
        this.totalRecords = 0
      },
    })
  }

  // Navigate to job-matcher with the selected job
  viewJobMatches(job: any) {
    this.jobService.setSelectedJob(job)
    this.router.navigate(["/job-matcher"])
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

  // Process files by uploading them to the server
  processFiles() {
    if (this.uploadedFiles.length === 0) {
      alert("Please select at least one file to upload")
      return
    }

    this.isUploading = true
    this.progress = 0
    this.uploadMessage = ""

    // Upload each file one by one
    this.uploadNextFile(0)
  }

  // Recursive function to upload files one by one
  uploadNextFile(index: number) {
    if (index >= this.uploadedFiles.length) {
      // All files have been uploaded
      this.isUploading = false
      this.uploadMessage = "All files uploaded successfully!"
      this.clearFiles()
      this.loadJobs() // Reload jobs to show the newly uploaded ones
      return
    }

    this.currentFile = this.uploadedFiles[index]
    this.progress = 0

    this.uploadService.uploadJobDescription(this.currentFile).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.progress = Math.round((100 * event.loaded) / event.total)
        } else if (event instanceof HttpResponse) {
          try {
            const response =  event.body
            if (response.message) {
              this.fileInfos.push({
                name: this.currentFile!.name,
                url: response.message.split("URL: ")[1],
                status: "Success",
              })
            } else if (response.error) {
              this.fileInfos.push({
                name: this.currentFile!.name,
                error: response.error,
                status: "Failed",
              })
            }
          } catch (e) {
            console.error("Error parsing response:", e)
            this.fileInfos.push({
              name: this.currentFile!.name,
              error: "Invalid server response",
              status: "Failed",
            })
          }

          // Upload the next file
          this.uploadNextFile(index + 1)
        }
      },
      error: (err: any) => {
        this.progress = 0
        this.uploadMessage = "Could not upload the file: " + this.currentFile?.name
        console.error("Upload error:", err)

        this.fileInfos.push({
          name: this.currentFile!.name,
          error: err.message,
          status: "Failed",
        })

        // Continue with the next file despite the error
        this.uploadNextFile(index + 1)
      },
    })
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

  // Updated to match backend expectations
  deleteSelectedJobs() {
    if (confirm("Are you sure you want to delete the selected jobs?")) {
      // Get all selected job IDs
      const selectedJobIds = this.jobs.filter((job) => job.selected).map((job) => job.id)

      if (selectedJobIds.length === 0) {
        alert("No jobs selected for deletion")
        return
      }

      this.isDeleting = true
      console.log("Deleting job IDs:", selectedJobIds)

      // Send the list of IDs to the backend
      this.jobService.deleteJobs(selectedJobIds).subscribe({
        next: (response) => {
          console.log("Delete response:", response)
          // Remove deleted jobs from the local array
          this.jobs = this.jobs.filter((job) => !job.selected)
          this.totalRecords = this.jobs.length
          this.isDeleting = false
          alert("Selected jobs have been deleted")
        },
        error: (error) => {
          console.error("Error deleting jobs:", error)
          this.isDeleting = false
          alert("Failed to delete jobs. Please try again.")
        },
      })
    }
  }

  getStatusDotClass(job: any): string {
    // This is a placeholder - in a real app, you'd have logic to determine if a job is active
    return "active"
  }

  getDepartment(job: any): string {
    // Extract department from job data
    const title = job?.jobTitle || ""

    if (
      title.includes("Developer") ||
      title.includes("Officer") ||
      title.includes("Engineer") ||
      title.includes("Tech") ||
      title.includes("IT")
    ) {
      return "Technology"
    } else if (title.includes("Marketing")) {
      return "Marketing"
    } else if (title.includes("Sales")) {
      return "Sales"
    } else {
      return "General"
    }
  }

  getDepartmentClass(job: any): string {
    if (!job) return "department-finance"

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

  getDepartmentIcon(job: any): string {
    if (!job) return "pi-briefcase"

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

  getMaxSalary(job: any): string {
    // This is a placeholder - in a real app, you'd have this data in your job model
    if (job?.salary && typeof job.salary === "string" && job.salary.includes("USD")) {
      const minSalary = Number.parseInt(job.salary.replace(/[^0-9]/g, ""))
      return `${minSalary + 10000} USD`
    }
    return "Negotiable"
  }

  onRowsChange() {
    // Handle rows per page change
    this.loadJobs()
  }
}
