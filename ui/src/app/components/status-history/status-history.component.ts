import { Component, input, OnChanges, SimpleChanges } from '@angular/core';
import { ServiceCard } from '../../models/service-card';

@Component({
  selector: 'app-service-history',
  templateUrl: './status-history.component.html',
  styleUrls: ['./status-history.component.scss'],
  standalone: false,
})
export class StatusHistoryComponent implements OnChanges {
  cards = input<ServiceCard[]>([]);
  first = 0;
  rows = 10;

  constructor() {}

  get totalRecords(): number {
    return this.cards().length;
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['cards']) {
      this.first = 0; // Reset pagination when data changes
    }
  }

  getPaginatedCards(): ServiceCard[] {
    const startIndex = this.first;
    const endIndex = Math.min(this.first + this.rows, this.cards().length);
    return this.cards().slice(startIndex, endIndex);
  }

  // eslint-disable-next-line
  onPageChange(event: any) {
    if (event.first !== undefined) this.first = event.first;
    if (event.rows !== undefined) this.rows = event.rows;
  }
}
