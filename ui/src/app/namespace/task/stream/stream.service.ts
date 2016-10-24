import {Injectable} from "@angular/core";
import {Http} from "@angular/http";
import "rxjs/add/operator/toPromise";
import {ConfigService, Identity} from "../../../rest.servcie";
import {FormItemBase, TextboxFormItem, TextareaFormItem, DynamicGroupFormItem} from "../../../dynamic/form/form-item";

export interface Stream extends Identity {
  ns?: string;
  task?: string;
  key?: string;
  name?: string;
  arguments?: string;
  template?: string;
}

@Injectable()
export class StreamService extends ConfigService<Stream> {
  constructor(public http: Http) {
    super(http);
  }

  getFormItems(): FormItemBase<any>[] {
    return [
      new TextboxFormItem({
        key: 'id',
        label: 'id',
        type: 'number',
        readonly: true,
        required: true
      }),
      new TextboxFormItem({
        key: 'key',
        label: 'key',
        required: true
      }),
      new TextboxFormItem({
        key: 'name',
        label: 'name',
        required: true
      }),
      new DynamicGroupFormItem({
        key: 'arguments',
        label: 'arguments'
      }),
      new TextareaFormItem({
        key: 'template',
        label: 'template'
      })
    ];
  }
}