import { Component, inject } from '@angular/core';
import { ServiceCard } from '../../models/service-card';
import { CpsDialogConfig, CpsDialogRef } from 'cps-ui-kit';

@Component({
  selector: 'app-service-card-details',
  templateUrl: './card-details.component.html',
  styleUrls: ['./card-details.component.scss'],
  standalone: false,
})
export class CardDetailsComponent {
  private _dialogRef = inject(CpsDialogRef);
  private _config = inject(CpsDialogConfig);

  card: ServiceCard | null = null;

  constructor() {
    this.card = this._config.data.card;
  }

  protected readonly JSON = JSON;
}
