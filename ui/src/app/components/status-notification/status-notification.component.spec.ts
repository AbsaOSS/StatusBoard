import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CpsNotificationAppearance } from 'cps-ui-kit';
import { MockModule } from 'ng-mocks';
import { SharedModule } from '../../modules/shared/shared.module';
import { NotificationPosition, NotificationTypeValues, StatusNotificationComponent } from './status-notification.component';

describe('StatusNotificationComponent', () => {
  let componentRef: ComponentRef<StatusNotificationComponent>;
  let component: StatusNotificationComponent;
  let fixture: ComponentFixture<StatusNotificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StatusNotificationComponent],
      imports: [NoopAnimationsModule, MockModule(SharedModule)],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusNotificationComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default values', () => {
    fixture.detectChanges();
    expect(component.filled).toBe(true);
    expect(component.color).toBe('error');
    expect(component.position).toBe(NotificationPosition.CENTER);
  });

  it('should set appearance from config', () => {
    componentRef.setInput('config', { appearance: CpsNotificationAppearance.OUTLINED });
    fixture.detectChanges();
    expect(component.filled).toBe(false);
  });

  it('should set filled from config', () => {
    componentRef.setInput('config', { appearance: CpsNotificationAppearance.FILLED });
    fixture.detectChanges();
    expect(component.filled).toBe(true);
  });

  it('should set color based on notification type', () => {
    componentRef.setInput('status', { type: NotificationTypeValues.Success });
    fixture.detectChanges();
    expect(component.color).toBe('success');
  });

  it('should set position from config', () => {
    componentRef.setInput('config', { position: NotificationPosition.LEFT });
    fixture.detectChanges();
    expect(component.position).toBe(NotificationPosition.LEFT);
  });

  it('should emit closed event when close is called', () => {
    const closedSpy = jest.spyOn(component.closed, 'emit');
    component.close();
    expect(closedSpy).toHaveBeenCalled();
  });

  it('should not emit refreshed event when refresh is called and disabled', () => {
    const refreshedSpy = jest.spyOn(component.refreshed, 'emit');
    componentRef.setInput('config', { refreshDisabled: true });
    component.refresh();
    expect(refreshedSpy).not.toHaveBeenCalled();
  });

  it('should display message and details', () => {
    componentRef.setInput('status', { message: 'Test Message', details: 'Test Details' });
    fixture.detectChanges();
    const messageElement = fixture.debugElement.query(By.css('.app-notification-message-header'));
    const detailsElement = fixture.debugElement.query(By.css('.app-notification-message-details'));
    expect(messageElement.nativeElement.textContent).toContain('Test Message');
    expect(detailsElement.nativeElement.textContent).toContain('Test Details');
  });

  it('should show close button when closable is true', () => {
    componentRef.setInput('config', { closable: true });
    fixture.detectChanges();
    const closeButton = fixture.debugElement.query(By.css('.app-notification-close-button'));
    expect(closeButton).toBeTruthy();
  });

  it('should not show close button when closable is false', () => {
    componentRef.setInput('config', { closable: false });
    fixture.detectChanges();
    const closeButton = fixture.debugElement.query(By.css('.app-notification-close-button'));
    expect(closeButton).toBeFalsy();
  });

  it('should show refresh button when refreshable is true', () => {
    componentRef.setInput('config', { refreshable: true });
    fixture.detectChanges();
    const refreshButton = fixture.debugElement.query(By.css('.app-notification-refresh-button'));
    expect(refreshButton).toBeTruthy();
  });

  it('should not show refresh button when refreshable is false', () => {
    componentRef.setInput('config', { refreshable: false });
    fixture.detectChanges();
    const refreshButton = fixture.debugElement.query(By.css('.app-notification-refresh-button'));
    expect(refreshButton).toBeFalsy();
  });

  it('should apply correct CSS classes based on notification type', () => {
    componentRef.setInput('status', { type: NotificationTypeValues.Warning });
    fixture.detectChanges();
    const notificationElement = fixture.debugElement.query(By.css('.app-notification-content'));
    expect(notificationElement.classes['warning']).toBeTruthy();
  });

  it('should clear timeout on component destroy', () => {
    // eslint-disable-next-line no-undef
    const clearTimeoutSpy = jest.spyOn(window, 'clearTimeout');
    // eslint-disable-next-line no-undef
    component.timeout = setTimeout(() => {}, 1000) as unknown as number;
    component.ngOnDestroy();
    expect(clearTimeoutSpy).toHaveBeenCalled();
  });
});
