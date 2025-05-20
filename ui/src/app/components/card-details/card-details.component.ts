import { Component } from '@angular/core';
import { ServiceCard } from '../../models/service-card';
import { CpsDialogConfig, CpsDialogRef } from 'cps-ui-kit';

@Component({
  selector: 'app-service-card-details',
  templateUrl: './card-details.component.html',
  styleUrls: ['./card-details.component.scss'],
  standalone: false,
})
export class CardDetailsComponent {
  card: ServiceCard | null = null;

  constructor(
    private _dialogRef: CpsDialogRef,
    private _config: CpsDialogConfig
  ) {
    this.card = this._config.data.card;
  }

  protected readonly JSON = JSON;
}
