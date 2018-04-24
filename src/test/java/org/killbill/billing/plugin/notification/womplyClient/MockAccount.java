package org.killbill.billing.plugin.notification.womplyClient;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.MutableAccountData;
import org.killbill.billing.catalog.api.Currency;

import java.util.UUID;

public class MockAccount implements Account {
    @Override
    public MutableAccountData toMutableAccountData() {
        return null;
    }

    @Override
    public Account mergeWithDelegate(Account delegate) {
        return null;
    }

    @Override
    public UUID getId() {
        return null;
    }

    @Override
    public DateTime getCreatedDate() {
        return null;
    }

    @Override
    public DateTime getUpdatedDate() {
        return null;
    }

    @Override
    public String getExternalKey() {
        return "AKEY";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Integer getFirstNameLength() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public Integer getBillCycleDayLocal() {
        return null;
    }

    @Override
    public Currency getCurrency() {
        return null;
    }

    @Override
    public UUID getPaymentMethodId() {
        return null;
    }

    @Override
    public DateTimeZone getTimeZone() {
        return null;
    }

    @Override
    public String getLocale() {
        return null;
    }

    @Override
    public String getAddress1() {
        return null;
    }

    @Override
    public String getAddress2() {
        return null;
    }

    @Override
    public String getCompanyName() {
        return null;
    }

    @Override
    public String getCity() {
        return null;
    }

    @Override
    public String getStateOrProvince() {
        return null;
    }

    @Override
    public String getPostalCode() {
        return null;
    }

    @Override
    public String getCountry() {
        return null;
    }

    @Override
    public String getPhone() {
        return null;
    }

    @Override
    public Boolean isMigrated() {
        return null;
    }

    @Override
    public Boolean isNotifiedForInvoices() {
        return null;
    }

    @Override
    public UUID getParentAccountId() {
        return null;
    }

    @Override
    public Boolean isPaymentDelegatedToParent() {
        return null;
    }

    @Override
    public DateTimeZone getFixedOffsetTimeZone() {
        return null;
    }

    @Override
    public DateTime getReferenceTime() {
        return null;
    }

    @Override
    public String getNotes() {
        return null;
    }
}
