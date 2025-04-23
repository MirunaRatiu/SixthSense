import { Component } from "@angular/core"
import { CommonModule } from "@angular/common"
import { RouterModule } from "@angular/router"
import { CardModule } from "primeng/card"
import { ButtonModule } from "primeng/button"
import { ChartModule } from "primeng/chart"
import { TableModule } from "primeng/table"
import { TagModule } from "primeng/tag"
import { AvatarModule } from "primeng/avatar"
import { AvatarGroupModule } from "primeng/avatargroup"
import { ProgressBarModule } from "primeng/progressbar"
import { KnobModule } from "primeng/knob"
import { FormsModule } from "@angular/forms"

@Component({
  selector: "app-menu",
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    ButtonModule,
    ChartModule,
    TableModule,
    TagModule,
    AvatarModule,
    AvatarGroupModule,
    ProgressBarModule,
    KnobModule,
    FormsModule,
  ],
  templateUrl: "./menu.component.html",
  styleUrls: ["./menu.component.scss"],
})
export class MenuComponent {
  chartData: any
  chartOptions: any

  jobsProgress = 65
  candidatesProgress = 78
  interviewsProgress = 42

  recentJobs = [
    {
      title: "Frontend Developer",
      company: "Tech Solutions Inc.",
      logo: "/placeholder.svg?height=32&width=32",
      applicants: 24,
      status: "Active",
    },
    {
      title: "UX Designer",
      company: "Creative Agency",
      logo: "/placeholder.svg?height=32&width=32",
      applicants: 18,
      status: "Active",
    },
    {
      title: "Backend Engineer",
      company: "Data Systems",
      logo: "/placeholder.svg?height=32&width=32",
      applicants: 12,
      status: "Closed",
    },
  ]

  topCandidates = [
    {
      name: "John Smith",
      avatar: "/placeholder.svg?height=32&width=32",
      role: "Frontend Developer",
      matchScore: 95,
    },
    {
      name: "Emily Johnson",
      avatar: "/placeholder.svg?height=32&width=32",
      role: "UX Designer",
      matchScore: 88,
    },
    {
      name: "Michael Brown",
      avatar: "/placeholder.svg?height=32&width=32",
      role: "Backend Engineer",
      matchScore: 82,
    },
  ]

  constructor() {
    this.initCharts()
  }

  initCharts() {
    const documentStyle = getComputedStyle(document.documentElement)
    const textColor = documentStyle.getPropertyValue("--text-color")
    const textColorSecondary = documentStyle.getPropertyValue("--text-color-secondary")
    const surfaceBorder = documentStyle.getPropertyValue("--surface-border")

    this.chartData = {
      labels: ["January", "February", "March", "April", "May", "June", "July"],
      datasets: [
        {
          label: "Jobs Posted",
          data: [28, 35, 42, 56, 48, 65, 72],
          fill: false,
          backgroundColor: documentStyle.getPropertyValue("--primary-color"),
          borderColor: documentStyle.getPropertyValue("--primary-color"),
          tension: 0.4,
        },
        {
          label: "Applications",
          data: [65, 82, 95, 120, 140, 132, 158],
          fill: false,
          backgroundColor: documentStyle.getPropertyValue("--primary-color-light"),
          borderColor: documentStyle.getPropertyValue("--primary-color-light"),
          tension: 0.4,
        },
      ],
    }

    this.chartOptions = {
      maintainAspectRatio: false,
      aspectRatio: 0.6,
      plugins: {
        legend: {
          labels: {
            color: textColor,
          },
        },
      },
      scales: {
        x: {
          ticks: {
            color: textColorSecondary,
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false,
          },
        },
        y: {
          ticks: {
            color: textColorSecondary,
          },
          grid: {
            color: surfaceBorder,
            drawBorder: false,
          },
        },
      },
    }
  }

  getStatusSeverity(status: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined {
    switch (status) {
      case "Active":
        return "success"
      case "Closed":
        return "danger"
      default:
        return "info"
    }
  }
}
