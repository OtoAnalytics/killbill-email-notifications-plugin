package org.killbill.billing.plugin.notification.womplyClient;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.invoice.api.InvoiceStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MockInvoice implements Invoice{
    @Override
    public boolean addInvoiceItem(InvoiceItem item) {
        return false;
    }

    @Override
    public boolean addInvoiceItems(Collection<InvoiceItem> items) {
        return false;
    }

    @Override
    public List<InvoiceItem> getInvoiceItems() {
        return null;
    }

    @Override
    public <T extends InvoiceItem> List<InvoiceItem> getInvoiceItems(Class<T> clazz) {
        return null;
    }

    @Override
    public int getNumberOfItems() {
        return 0;
    }

    @Override
    public boolean addPayment(InvoicePayment payment) {
        return false;
    }

    @Override
    public boolean addPayments(Collection<InvoicePayment> payments) {
        return false;
    }

    @Override
    public List<InvoicePayment> getPayments() {
        return null;
    }

    @Override
    public int getNumberOfPayments() {
        return 0;
    }

    @Override
    public UUID getAccountId() {
        return null;
    }

    @Override
    public Integer getInvoiceNumber() {
        return 123;
    }

    @Override
    public LocalDate getInvoiceDate() {
        return null;
    }

    @Override
    public LocalDate getTargetDate() {
        return null;
    }

    @Override
    public Currency getCurrency() {
        return null;
    }

    @Override
    public BigDecimal getPaidAmount() {
        return null;
    }

    @Override
    public BigDecimal getOriginalChargedAmount() {
        return null;
    }

    @Override
    public BigDecimal getChargedAmount() {
        return null;
    }

    @Override
    public BigDecimal getCreditedAmount() {
        return null;
    }

    @Override
    public BigDecimal getRefundedAmount() {
        return null;
    }

    @Override
    public BigDecimal getBalance() {
        return null;
    }

    @Override
    public boolean isMigrationInvoice() {
        return false;
    }

    @Override
    public InvoiceStatus getStatus() {
        return null;
    }

    @Override
    public boolean isParentInvoice() {
        return false;
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
}
