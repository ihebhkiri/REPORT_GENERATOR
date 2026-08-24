import {Component, computed, effect, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import { ProgressBarModule } from 'primeng/progressbar';

@Component({
  selector: 'app-iheb',
  imports: [
    FormsModule,ProgressBarModule
  ],
  templateUrl: './iheb.component.html',
  styleUrl: './iheb.component.scss',
})
export class IhebComponent {
  name = signal<string>('')
  lastName = signal<string>('')

  constructor() {
    console.log('iheb component constructor');
    effect(() => {
      console.log("fullName " + this.fullName());
    });
  }

  fullName = computed(() => this.name() + " " + this.lastName());

}
