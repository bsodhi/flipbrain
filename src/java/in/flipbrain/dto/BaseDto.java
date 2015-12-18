/*
Copyright 2015 Balwinder Sodhi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package in.flipbrain.dto;

import java.util.Date;

/**
 *
 * @author Balwinder Sodhi
 */
public abstract class BaseDto {
    
    private int count;
    private boolean deleted;
    private boolean dirty;
    private int txnNo;
    private Date txnTs;
    private String appUser;
    private Date updTs;
    private Date insTs;

    public Date getUpdTs() {
        return updTs;
    }

    public void setUpdTs(Date updTs) {
        this.updTs = updTs;
    }

    public Date getInsTs() {
        return insTs;
    }

    public void setInsTs(Date insTs) {
        this.insTs = insTs;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public int getTxnNo() {
        return txnNo;
    }

    public void setTxnNo(int txnNo) {
        this.txnNo = txnNo;
    }

    public Date getTxnTs() {
        return txnTs;
    }

    public void setTxnTs(Date txnTs) {
        this.txnTs = txnTs;
    }

    public String getAppUser() {
        return appUser;
    }

    public void setAppUser(String appUser) {
        this.appUser = appUser;
    }
    
}
