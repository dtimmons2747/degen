import { CommonModule } from '@angular/common';
import { Component, HostListener, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-side-menu',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './side-menu.component.html',
  styleUrl: './side-menu.component.scss'
})
export class SideMenuComponent {
  isMobile = signal(false);

  constructor() {
    this.checkIsMobile();
  }

  @HostListener('window:resize', ['$event'])
  onResize(event: any) {
    this.checkIsMobile();
  }

  private checkIsMobile() {
    this.isMobile.set(window.innerWidth <= 768);
  }
}
