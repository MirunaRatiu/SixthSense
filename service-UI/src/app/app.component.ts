import { Component } from "@angular/core"
import { CommonModule } from "@angular/common"
import { RouterOutlet } from "@angular/router"
import { SidebarComponent } from "./components/sidebar/sidebar.component"
import { HeaderComponent } from "./components/header/header.component"

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  standalone: true
})
export class AppComponent {
  title = 'accesaPROJECT';
  sidebarVisible = true

  toggleSidebar() {
    this.sidebarVisible = !this.sidebarVisible
  }
}
