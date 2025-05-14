import { ComponentRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { CpsPaginatePipe } from 'cps-ui-kit';
import { MockModule, MockPipe, ngMocks } from 'ng-mocks';
import { SharedModule } from '../../modules/shared/shared.module';
import { StatusHistoryComponent } from './status-history.component';
import { makeTestCard } from '../../test.data';

describe('StatusHistoryComponent', () => {
  let component: StatusHistoryComponent;
  let fixture: ComponentFixture<StatusHistoryComponent>;
  let componentRef: ComponentRef<StatusHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StatusHistoryComponent, MockPipe(CpsPaginatePipe)],
      imports: [MockModule(SharedModule), ReactiveFormsModule],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusHistoryComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty cards', () => {
    expect(component.cards()).toEqual([]);
  });

  it('should update cards', () => {
    const testCards = [makeTestCard('TstSvc1'), makeTestCard('TstSvc2')];
    componentRef.setInput('cards', testCards);
    fixture.detectChanges();

    expect(component.cards()).toEqual(testCards);
    expect(component.totalRecords).toBe(testCards.length);
  });

  it('should display cards', fakeAsync(() => {
    const testCards = [makeTestCard('TstSvc1'), makeTestCard('TstSvc2')];
    componentRef.setInput('cards', testCards);
    fixture.detectChanges();

    const serviceCards = ngMocks.findAll('.service-card');
    expect(serviceCards.length).toBe(testCards.length);
  }));

  it('should update pagination on page change', () => {
    const mockEvent = { first: 10, rows: 20 };
    component.onPageChange(mockEvent);

    expect(component.first).toBe(10);
    expect(component.rows).toBe(20);
  });

  it('should reset first page on cards change', () => {
    const testCards = [makeTestCard('TstSvc1'), makeTestCard('TstSvc2')];
    component.first = 10; // Set a non-zero initial value
    componentRef.setInput('cards', testCards);

    fixture.detectChanges();
    expect(component.first).toBe(0);
  });
});
