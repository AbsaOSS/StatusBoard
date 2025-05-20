import { Component, input, signal } from '@angular/core';
import { ServiceCard } from '../../models/service-card';
import { RefreshService } from '../../services/refresh/refresh.service';

@Component({
  selector: 'app-service-status',
  templateUrl: './status-list.component.html',
  styleUrls: ['./status-list.component.scss'],
  standalone: false,
})
export class StatusListComponent {
  cards = input<ServiceCard[]>([]);
  isLoading = signal(false);

  constructor(private refreshService: RefreshService) {
    this.refreshService.refreshPending$.subscribe(loading => {
      this.isLoading.set(loading);
    });
  }
}
