import { Component } from '@angular/core';
import { HierarchicalServiceCard, ServiceCard } from '../../models/service-card';
import { CpsDialogConfig, CpsDialogRef } from 'cps-ui-kit';

@Component({
  selector: 'app-service-dependency-graph',
  templateUrl: './dependency-graph.component.html',
  styleUrls: ['./dependency-graph.component.scss'],
  standalone: false,
})
export class DependencyGraphComponent {
  dependenciesCards: HierarchicalServiceCard[] = [];
  dependentsCards: HierarchicalServiceCard[] = [];
  historyCards: ServiceCard[] = [];

  constructor(
    private _dialogRef: CpsDialogRef,
    private _config: CpsDialogConfig
  ) {
    this.dependenciesCards = this._config.data.dependenciesCards;
    this.dependentsCards = this._config.data.dependentsCards;
    this.historyCards = this._config.data.historyCards;
  }
}
