/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Component, OnInit, ViewChild} from '@angular/core';
import { DomainService } from "../../../../services/domain.service";
import { DialogService } from "../../../../services/dialog.service";
import { ActivatedRoute, Router } from "@angular/router";
import { SnackbarService } from "../../../../services/snackbar.service";
import { BreadcrumbService } from "../../../../../libraries/ng2-breadcrumb/components/breadcrumbService";
import { SidenavService } from "../../../../components/sidenav/sidenav.service";
import { ClientService } from "../../../../services/client.service";
import { MatInput } from "@angular/material";

export interface Client {
  id: string;
  clientId: string;
}

@Component({
  selector: 'app-openid-client-registration',
  templateUrl: './client-registration.component.html',
  styleUrls: ['./client-registration.component.scss']
})
export class DomainSettingsOpenidClientRegistrationComponent implements OnInit {

  @ViewChild('chipInput') chipInput: MatInput;

  formChanged: boolean = false;
  domain: any = {};
  clients: Client[];
  hasClients: boolean = false;
  clientDcrDisabled: boolean = false;
  disableToolTip: boolean = false;
  toolTipMessage = "";

  constructor(private domainService: DomainService, private dialogService: DialogService, private snackbarService: SnackbarService,
              private router: Router, private route: ActivatedRoute, private breadcrumbService: BreadcrumbService, private sidenavService: SidenavService,
              private clientService: ClientService) {
  }

  ngOnInit() {
    this.domain = this.route.snapshot.parent.data['domain'];
    this.initClients();//this.clientService.findByDomain(this.domain.id).map(res => res.json()).subscribe(data =>  this.initClients(data));
    this.canUpdateClients();
  }

  initClients() {
    this.clientService.findByDomain(this.domain.id).map(res => res.json()).subscribe(clientList =>  {
      this.clients = clientList;//_.filter(clientList, function(client) { return client.dynamicClientRegistrationEnabled===true; });
      this.hasClients = this.clients !== undefined && this.clients.length>0;
    });
  }

  canUpdateClients() {
    this.clientDcrDisabled = !this.domain.oidc.dynamicClientRegistration.enabled; //Disable toogle only if dcr is not enabled.
    this.disableToolTip = this.domain.oidc.dynamicClientRegistration.enabled;//Disable tooltip if dcr is enabled.
    if(!this.disableToolTip) {this.toolTipMessage = "This feature is not enabled on your domain.";}
  }

  enableDynamicClientRegistration(event) {
    this.domain.oidc.dynamicClientRegistration.enabled = event.checked;
    //If disabled, ensure to disable open dynamic client registration too and disable clients toogle too.
    if(!event.checked) {
      this.domain.oidc.dynamicClientRegistration.openRegistrationEnabled = event.checked;
      this.clientDcrDisabled = !event.checked;
      this.disableToolTip = event.checked;
      this.toolTipMessage = "Disable until settings are saved and feature is enabled.";
    }
    this.formChanged = true;
  }

  enableOpenDynamicClientRegistration(event) {
    this.domain.oidc.dynamicClientRegistration.openRegistrationEnabled = event.checked;
    //If enabled, ensure to enable dynamic client registration too.
    if(event.checked) {
      this.domain.oidc.dynamicClientRegistration.enabled = event.checked;
    }
    this.formChanged = true;
  }

  allowLocalhostRedirectUri(event) {
    this.domain.oidc.dynamicClientRegistration.allowLocalhostRedirectUri = event.checked;
    this.formChanged = true;
  }

  allowHttpSchemeRedirectUri(event) {
    this.domain.oidc.dynamicClientRegistration.allowHttpSchemeRedirectUri = event.checked;
    this.formChanged = true;
  }

  allowWildCardRedirectUri(event) {
    this.domain.oidc.dynamicClientRegistration.allowWildCardRedirectUri = event.checked;
    this.formChanged = true;
  }

  patch() {
    this.domainService.patchOpenidDCRSettings(this.domain.id, this.domain).subscribe(response => {
      this.domain = response.json();
      this.domainService.notify(this.domain);
      this.sidenavService.notify(this.domain);
      this.breadcrumbService.addFriendlyNameForRoute('/domains/'+this.domain.id, this.domain.name);
      this.snackbarService.open("Domain " + this.domain.name + " updated");
    });
    this.canUpdateClients();
  }

  patchClientDcrSettings(id, event) {
    this.clientService.enableDynamicClientRegistration(this.domain.id, id, event.checked).map(res => res.json())
      .subscribe(response => {
        this.snackbarService.open("Client updated");
      }
    );

  }
}
