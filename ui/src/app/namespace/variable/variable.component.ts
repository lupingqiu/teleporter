import {Component, OnInit} from "@angular/core";
import {ActivatedRoute} from "@angular/router";
import {VariableService, VariableRuntimeService, Variable} from "./variable.service";
import {Location} from "@angular/common";
import {KeyBean} from "../../rest.servcie";
import {FormItemService} from "../../dynamic/form/form-item.service";
import {FormItemBase} from "../../dynamic/form/form-item";
import {FormGroup, FormControl} from "@angular/forms";

@Component({
  selector: 'variable-list',
  templateUrl: './variable-list.component.html'
})
export class VariableListComponent implements OnInit {
  private kbs: KeyBean<Variable>[] = [];
  private ns: string;

  constructor(private variableService: VariableService, private variableRuntimeService: VariableRuntimeService, private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.ns = params['ns'];
      this.list();
    })
  }

  list() {
    this.variableService.range(`/variable/${this.ns}`, 0, 2000)
      .then((kbs: KeyBean<Variable>[]) => this.kbs = kbs);
  }

  del(key: string) {
    if (window.confirm("Are you sure delete it !!!")) {
      this.variableRuntimeService.remove(key);
      this.variableService.remove(key).then(() => this.list());
    }
  }

  refresh(key: string) {
    this.variableService.refresh(key);
  }

  runtime(i: number, key: string) {
    this.variableRuntimeService.findOne(key)
      .then(kv => this.kbs[i].value.runtime = kv.value);
  }
}

@Component({
  selector: 'variable-detail',
  templateUrl: './variable-detail.component.html'
})
export class VariableDetailComponent implements OnInit {
  private formItems: FormItemBase<any>[];
  private formGroup: FormGroup = new FormGroup({"": new FormControl()});
  private payLoad: string;
  private key: string;
  private ns: string;

  constructor(private route: ActivatedRoute, private location: Location,
              private variableService: VariableService, private formItemService: FormItemService) {
  }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.ns = params['ns'];
      let variable = params['variable'];
      if (variable) {
        this.variableService.findOne(this.fullKey(variable))
          .then((kb: KeyBean<Variable>) => {
            this.key = kb.value.key;
            this.setForm(kb.value);
          });
      } else {
        this.setForm({});
      }
    })
  }

  setForm(value: any) {
    let form = this.formItemService.toForm(this.variableService.getFormItems(), value);
    this.formItems = form.formItems;
    this.formGroup = form.formGroup;
  }

  preview() {
    this.payLoad = JSON.stringify(this.formGroup.value, null, '\t');
  }

  onSubmit() {
    let variable = this.formGroup.value;
    this.variableService.save(this.fullKey(variable.key), variable)
      .then(v => this.location.back());
  }

  private fullKey(key: string) {
    return `/variable/${this.ns}/${key}`;
  }
}