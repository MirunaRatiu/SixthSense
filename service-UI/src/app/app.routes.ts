import type { Routes } from "@angular/router"

export const routes: Routes = [
  {
    path: "menu",
    loadComponent: () => import("./pages/menu/menu.component").then((m) => m.MenuComponent),
  },
  {
    path: "jobs",
    loadComponent: () => import("./pages/jobs/jobs.component").then((m) => m.JobsComponent),
  },
  {
    path: "candidates",
    loadComponent: () => import("./pages/candidates/candidates.component").then((m) => m.CandidatesComponent),
  },
  {
    path: "job-matcher",
    loadComponent: () => import("./pages/job-matcher/job-matcher.component").then((m) => m.JobMatcherComponent),
  },
  {
    path: "cv-matcher",
    loadComponent: () => import("./pages/cv-matcher/cv-matcher.component").then((m) => m.CvMatcherComponent),
  },

  { path: "", redirectTo: "/jobs", pathMatch: "full" },
]
