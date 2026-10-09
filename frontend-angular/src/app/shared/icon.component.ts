import { Component, Input } from '@angular/core';
@Component({
  selector: 'app-icon', standalone: true,
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="paths[name] || paths[\'home\']" /></svg>',
  styles: [':host { display:inline-flex; width:20px; height:20px; flex-shrink:0; vertical-align:middle } svg { width:100%; height:100% }']
})
export class IconComponent {
  @Input() name = 'home';
  paths: Record<string, string> = {
    home: 'M3 10 12 3l9 7v11h-6v-7H9v7H3Z',
    calculator: 'M5 3h14v18H5ZM8 6h8v4H8ZM8 14h1m6 0h1m-8 3h1m6 0h1',
    folder: 'M3 5h7l2 3h9v12H3Zm5 8h8m-8 3h5',
    arrow: 'M5 12h14m-5-5 5 5-5 5', plus: 'M12 5v14M5 12h14',
    check: 'm5 12 4 4L19 6',
    shield: 'M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6Zm-4 9 3 3 5-6',
    user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-3a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v3',
    info: 'M12 11v6m0-10v.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
    search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    trash: 'M3 6h18M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7',
    clock: 'M12 6v6l4 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0',
    close: 'm6 6 12 12M6 18 18 6', chart: 'M4 3v17h17M8 15v-4m5 4V7m5 8V4',
    lock: 'M6 10h12v11H6Zm2 0V7a4 4 0 0 1 8 0v3'
  };
}
