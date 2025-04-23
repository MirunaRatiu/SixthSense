import { Component, EventEmitter, Output } from "@angular/core"
import { CommonModule } from "@angular/common"
import { RouterModule } from "@angular/router"
import { ButtonModule } from "primeng/button"
import { InputTextModule } from "primeng/inputtext"
import { MenubarModule } from "primeng/menubar"
import { AvatarModule } from "primeng/avatar"
import { MenuModule } from 'primeng/menu';
import { DividerModule } from 'primeng/divider';

@Component({
  selector: "app-header",
  standalone: true,
  imports: [    CommonModule,
    RouterModule,
    ButtonModule,
    InputTextModule,
    MenubarModule,
    AvatarModule,
    MenuModule,
    DividerModule,
  ],
  templateUrl: "./header.component.html",
  styleUrls: ["./header.component.css"],
})
export class HeaderComponent {
  @Output() sidebarToggle = new EventEmitter<void>()

  toggleSidebar() {
    this.sidebarToggle.emit()
  }
}
