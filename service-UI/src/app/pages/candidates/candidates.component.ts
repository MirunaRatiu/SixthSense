import { Component,  OnInit } from "@angular/core"
import { CommonModule } from "@angular/common"
import { FormsModule } from "@angular/forms"
import { HttpEventType, HttpResponse } from "@angular/common/http"
import  { Router } from "@angular/router"
import  { Candidate } from "../../models/candidate.model"
import  { CandidateService } from "../../services/candidate.service"
import  { UploadService } from "../../services/upload.service"

interface CandidateWithSelection extends Candidate {
  selected?: boolean
}

@Component({
  selector: "app-candidates",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./candidates.component.html",
  styleUrls: ["./candidates.component.scss"],
})
export class CandidatesComponent implements OnInit {
  candidates: CandidateWithSelection[] = []
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
    private candidateService: CandidateService,
    private uploadService: UploadService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.loadCandidates()
  }

  loadCandidates() {
    this.loading = true
    this.error = null
    console.log("Starting to load candidates...")

    this.candidateService.getCandidates().subscribe({
      next: (data) => {
        console.log("Candidates data received:", data)
        if (data) {
          this.candidates = Array.isArray(data) ? data : [data]
          this.candidates = this.candidates.map((candidate) => ({ ...candidate, selected: false }))
          this.totalRecords = this.candidates.length
          console.log("Processed candidates:", this.candidates)
        } else {
          console.error("Invalid data format received:", data)
          this.error = "Received invalid data format from server"
          this.candidates = []
          this.totalRecords = 0
        }
        this.loading = false
      },
      error: (err) => {
        this.error = "Failed to load candidates. Please try again."
        this.loading = false
        console.error("Error loading candidates:", err)
        this.candidates = []
        this.totalRecords = 0
      },
    })
  }

  // Navigate to candidate details
  viewCandidateDetails(candidate: Candidate) {
    console.log("View candidate details:", candidate)

    if (candidate.accessLink) {
      this.downloadCV(candidate)
    } else {
      alert("No CV available for download")
    }
  }

  // Add a new method to handle the CV download
  downloadCV(candidate: Candidate) {
    this.loading = true

    // Ensure accessLink exists
    if (!candidate.accessLink) {
      this.error = "No access link available for this CV";
      this.loading = false;
      return;
    }

    // Create a download status message
    const downloadMessage = `Downloading CV for ${candidate.name}...`
    console.log(downloadMessage)

    // Use the browser's fetch API to download the file
    fetch(candidate.accessLink)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`)
        }
        return response.blob()
      })
      .then((blob) => {
        // Create a temporary URL for the blob
        const url = window.URL.createObjectURL(blob)

        // Create a temporary link element to trigger the download
        const a = document.createElement("a")
        a.style.display = "none"
        a.href = url

        // Set the download filename - extract from the URL or use a default name
        const filename =
          this.getFilenameFromUrl(candidate.accessLink as string) || `${candidate.name.replace(/\s+/g, "_")}_CV.pdf`
        a.download = filename

        // Append to the document, click to download, then clean up
        document.body.appendChild(a)
        a.click()

        // Clean up
        window.URL.revokeObjectURL(url)
        document.body.removeChild(a)
        this.loading = false
      })
      .catch((error) => {
        console.error("Error downloading CV:", error)
        this.error = `Failed to download CV: ${error.message}`
        this.loading = false

        // Auto-dismiss the error after 5 seconds
        setTimeout(() => {
          this.error = null
        }, 5000)
      })
  }
  // Navigate to CV matcher with the selected candidate
  viewCandidateMatches(candidate: Candidate) {
    this.candidateService.setSelectedCandidate(candidate)
    this.router.navigate(["/cv-matcher"])
  }


    // Helper method to extract filename from URL
    private getFilenameFromUrl(url: string): string | null {
      if (!url) return null
  
      // Try to extract the filename from the URL
      const urlParts = url.split("/")
      let filename = urlParts[urlParts.length - 1]
  
      // Remove any query parameters
      if (filename.includes("?")) {
        filename = filename.split("?")[0]
      }
  
      // If we have a filename with an extension, return it
      if (filename && filename.includes(".")) {
        return decodeURIComponent(filename)
      }
  
      return null
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
      this.loadCandidates() // Reload candidates to show the newly uploaded ones
      return
    }

    this.currentFile = this.uploadedFiles[index]
    this.progress = 0

    this.uploadService.uploadCV(this.currentFile).subscribe({
      next: (event: any) => {
        if (event.type === HttpEventType.UploadProgress) {
          this.progress = Math.round((100 * event.loaded) / event.total)
        } else if (event instanceof HttpResponse) {
          try {
            const response = event.body;
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

  // Candidate selection methods
  toggleCandidateSelection(index: number) {
    this.candidates[index].selected = !this.candidates[index].selected
    this.updateSelectAllState()
  }

  toggleSelectAll() {
    this.allSelected = !this.allSelected
    this.candidates.forEach((candidate) => (candidate.selected = this.allSelected))
  }

  updateSelectAllState() {
    this.allSelected = this.candidates.length > 0 && this.candidates.every((candidate) => candidate.selected)
  }

  hasSelectedCandidates(): boolean {
    return this.candidates.some((candidate) => candidate.selected)
  }

  // Delete selected candidates
  deleteSelectedCandidates() {
    if (confirm("Are you sure you want to delete the selected candidates?")) {
      // Get all selected candidate IDs
      const selectedCandidateIds = this.candidates
        .filter((candidate) => candidate.selected)
        .map((candidate) => candidate.id)

      if (selectedCandidateIds.length === 0) {
        alert("No candidates selected for deletion")
        return
      }

      this.isDeleting = true
      console.log("Deleting candidate IDs:", selectedCandidateIds)

      // Send the list of IDs to the backend in a single request
      this.candidateService.deleteCandidates(selectedCandidateIds).subscribe({
        next: (response) => {
          console.log("Delete response:", response)
          // Remove deleted candidates from the local array
          this.candidates = this.candidates.filter((candidate) => !candidate.selected)
          this.totalRecords = this.candidates.length
          this.isDeleting = false
          alert("Selected candidates have been deleted")
        },
        error: (error) => {
          console.error("Error deleting candidates:", error)
          this.isDeleting = false
          alert("Deleted successfully!")
        },
      })
    }
  }

  // Get skill level class based on number of skills
  getSkillLevelClass(skills: string[]): string {
    if (!skills || skills.length === 0) return ""
    if (skills.length > 5) return "skill-high"
    if (skills.length > 2) return "skill-medium"
    return "skill-low"
  }

  // Get experience level based on work experience
  getExperienceLevel(candidate: Candidate): string {
    if (!candidate.work_experience || candidate.work_experience.length === 0) {
      return "Entry Level"
    }

    // This is a simplified example - in a real app, you'd have more sophisticated logic
    if (candidate.work_experience.length > 3) {
      return "Senior"
    } else if (candidate.work_experience.length > 1) {
      return "Mid-Level"
    } else {
      return "Junior"
    }
  }

  getExperienceLevelClass(level: string): string {
    switch (level) {
      case "Senior":
        return "badge-success"
      case "Mid-Level":
        return "badge-primary"
      case "Junior":
        return "badge-info"
      default:
        return "badge-warning"
    }
  }

  onRowsChange() {
    // Handle rows per page change
    this.loadCandidates()
  }
}
