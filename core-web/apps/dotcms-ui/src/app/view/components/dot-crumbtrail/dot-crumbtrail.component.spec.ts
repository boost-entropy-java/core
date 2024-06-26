import { Observable, Subject } from 'rxjs';

import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { BreadcrumbModule } from 'primeng/breadcrumb';

import { DotCrumbtrailComponent } from './dot-crumbtrail.component';
import { DotCrumb, DotCrumbtrailService } from './service/dot-crumbtrail.service';

@Injectable()
class MockDotCrumbtrailService {
    private crumbTrail: Subject<DotCrumb[]> = new Subject();

    get crumbTrail$(): Observable<DotCrumb[]> {
        return this.crumbTrail.asObservable();
    }

    trigger(crumbs: DotCrumb[]): void {
        this.crumbTrail.next(crumbs);
    }
}

describe('DotCrumbtrailComponent', () => {
    let fixture: ComponentFixture<DotCrumbtrailComponent>;
    let de: DebugElement;
    const dotCrumbtrailService: MockDotCrumbtrailService = new MockDotCrumbtrailService();

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            declarations: [DotCrumbtrailComponent],
            imports: [BreadcrumbModule],
            providers: [
                {
                    provide: DotCrumbtrailService,
                    useValue: dotCrumbtrailService
                }
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotCrumbtrailComponent);
        de = fixture.debugElement;

        fixture.detectChanges();
    });

    it('should has a p-breadcrumb', () => {
        const pBreadCrumb: DebugElement = de.query(By.css('p-breadcrumb'));
        expect(pBreadCrumb).not.toBeNull();
    });

    it('should listen crumbTrail event from service', () => {
        const crumbs = [
            {
                label: 'label',
                url: 'url'
            }
        ];

        dotCrumbtrailService.trigger(crumbs);

        const pBreadCrumb: DebugElement = de.query(By.css('p-breadcrumb'));

        fixture.detectChanges();
        expect(pBreadCrumb.componentInstance.model).toBe(crumbs);
    });
});
