import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import {
  CpsButtonComponent,
  CpsDividerComponent,
  CpsIconComponent,
  CpsInputComponent,
  CpsNotificationService,
  CpsPaginatePipe,
  CpsPaginatorComponent,
  CpsProgressCircularComponent,
  CpsTooltipDirective,
} from 'cps-ui-kit';
// import { CpsToastComponent } from 'cps-ui-kit/lib/services/cps-notification/internal/components/cps-toast/cps-toast.component';
import { CardModule } from 'primeng/card';

@NgModule({
  imports: [
    CommonModule,
    CardModule,
    CpsDividerComponent,
    CpsIconComponent,
    CpsInputComponent,
    CpsProgressCircularComponent,
    CpsPaginatePipe,
    // CpsToastComponent,
    CpsTooltipDirective,
    CpsButtonComponent,
    CpsPaginatorComponent,
  ],
  providers: [CpsNotificationService],
  exports: [
    CommonModule,
    CardModule,
    CpsDividerComponent,
    CpsIconComponent,
    CpsInputComponent,
    CpsProgressCircularComponent,
    CpsPaginatePipe,
    // CpsToastComponent,
    CpsTooltipDirective,
    CpsButtonComponent,
    CpsPaginatorComponent,
  ],
})
export class SharedModule {}
