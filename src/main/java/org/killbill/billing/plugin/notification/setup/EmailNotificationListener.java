/*
 * Copyright 2015-2015 Groupon, Inc
 * Copyright 2015-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.notification.setup;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.samskivert.mustache.MustacheException;
import org.apache.commons.mail.EmailException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountEmail;
import org.killbill.billing.catalog.api.BillingActionPolicy;
import org.killbill.billing.catalog.api.PlanPhasePriceOverride;
import org.killbill.billing.catalog.api.PlanPhaseSpecifier;
import org.killbill.billing.entitlement.api.Entitlement;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.entitlement.api.SubscriptionEventType;
import org.killbill.billing.invoice.api.DryRunArguments;
import org.killbill.billing.invoice.api.DryRunType;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceApiException;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.InvoicePayment;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionStatus;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.notification.email.EmailContent;
import org.killbill.billing.plugin.notification.email.EmailSender;
import org.killbill.billing.plugin.notification.generator.ResourceBundleFactory;
import org.killbill.billing.plugin.notification.generator.TemplateRenderer;
import org.killbill.billing.plugin.notification.templates.MustacheTemplateEngine;
import org.killbill.billing.plugin.notification.womplyClient.SubscriptionClientImpl;
import org.killbill.billing.tenant.api.TenantApiException;
import org.killbill.billing.util.api.TagDefinitionApiException;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.tag.Tag;
import org.killbill.billing.util.tag.TagDefinition;
import org.osgi.service.log.LogService;
import org.skife.config.TimeSpan;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public class EmailNotificationListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final String INVOICE_DRY_RUN_TIME_PROPERTY = "org.killbill.invoice.dryRunNotificationSchedule";
    private static final String SUBSCRIPTION_SERVICE_ADDRESS = "org.killbill.billing.plugin.notification.email.subscription.address";
    private static final int SUBSCRIPTION_SERVICE_READ_TIMEOUT = 10;
    private static final int SUBSCRIPTION_SERVICE_CONNECT_TIMEOUT = 10;

    private static final NullDryRunArguments NULL_DRY_RUN_ARGUMENTS = new NullDryRunArguments();

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;
    private final TemplateRenderer templateRenderer;
    private final OSGIConfigPropertiesService configProperties;
    private final EmailSender emailSender;
    private final OSGIKillbillClock clock;
    private final SubscriptionClientImpl subscriptionClient;


    private final ImmutableList<ExtBusEventType> EVENTS_TO_CONSIDER = new ImmutableList.Builder()
            .add(ExtBusEventType.INVOICE_NOTIFICATION)
            .add(ExtBusEventType.INVOICE_CREATION)
            .add(ExtBusEventType.INVOICE_PAYMENT_SUCCESS)
            .add(ExtBusEventType.INVOICE_PAYMENT_FAILED)
            .add(ExtBusEventType.SUBSCRIPTION_CANCEL)
            .build();


    public EmailNotificationListener(final OSGIKillbillClock clock, final OSGIKillbillLogService logService, final OSGIKillbillAPI killbillAPI, final OSGIConfigPropertiesService configProperties) {
        this.logService = logService;
        this.osgiKillbillAPI = killbillAPI;
        this.configProperties = configProperties;
        this.clock = clock;
        this.emailSender = new EmailSender(configProperties, logService);
        this.templateRenderer = new TemplateRenderer(new MustacheTemplateEngine(), new ResourceBundleFactory(killbillAPI.getTenantUserApi(), logService), killbillAPI.getTenantUserApi(), logService);

        String subscriptionAddress = configProperties.getString(SUBSCRIPTION_SERVICE_ADDRESS);
        Preconditions.checkArgument(subscriptionAddress != null, String.format("Cannot find property %s", SUBSCRIPTION_SERVICE_ADDRESS));

        this.subscriptionClient = new SubscriptionClientImpl(subscriptionAddress, logService, SUBSCRIPTION_SERVICE_READ_TIMEOUT, SUBSCRIPTION_SERVICE_CONNECT_TIMEOUT);
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {

        if (!EVENTS_TO_CONSIDER.contains(killbillEvent.getEventType())) {
            return;
        }

        // TODO see https://github.com/killbill/killbill-platform/issues/5
        final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), new EmailNotificationContext(killbillEvent.getTenantId()));
            final String to = account.getEmail();
            if (to == null) {
                logService.log(LogService.LOG_INFO, "Account " + account.getId() + " does not have an email address configured, skip...");
                return;
            }

            final EmailNotificationContext context = new EmailNotificationContext(killbillEvent.getTenantId());
            switch (killbillEvent.getEventType()) {
                case INVOICE_NOTIFICATION:
                    sendEmailForUpComingInvoice(account, killbillEvent, context);
                    break;

                case INVOICE_CREATION:
                case INVOICE_PAYMENT_SUCCESS:
                case INVOICE_PAYMENT_FAILED:
                    sendEmailForPayment(account, killbillEvent, context);
                    break;

                case SUBSCRIPTION_CANCEL:
                    sendEmailForCancelledSubscription(account, killbillEvent, context);
                    break;

                default:
                    break;
            }

            logService.log(LogService.LOG_INFO, String.format("Received event %s for object type = %s, id = %s",
                    killbillEvent.getEventType(), killbillEvent.getObjectType(), killbillEvent.getObjectId()));

        } catch (final AccountApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Unable to find account: %s", killbillEvent.getAccountId()), e);
        } catch (InvoiceApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to retrieve invoice for account %s", killbillEvent.getAccountId()), e);
        } catch (SubscriptionApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to retrieve subscription for account %s", killbillEvent.getAccountId()), e);
        } catch (PaymentApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to send email for account %s", killbillEvent.getAccountId()), e);
        } catch (EmailException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to send email for account %s", killbillEvent.getAccountId()), e);
        } catch (IOException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to send email for account %s", killbillEvent.getAccountId()), e);
        } catch (TenantApiException e) {
            logService.log(LogService.LOG_WARNING, String.format("Fail to send email for account %s", killbillEvent.getAccountId()), e);
        } catch (IllegalArgumentException e) {
            logService.log(LogService.LOG_WARNING, e.getMessage(), e);
        } catch (MustacheException e) {
            logService.log(LogService.LOG_WARNING, e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }

    private void sendEmailForUpComingInvoice(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws IOException, InvoiceApiException, EmailException, TenantApiException {

        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.INVOICE_NOTIFICATION, String.format("Unexpected event %s", killbillEvent.getEventType()));

        final String dryRunTimePropValue = configProperties.getString(INVOICE_DRY_RUN_TIME_PROPERTY);
        Preconditions.checkArgument(dryRunTimePropValue != null, String.format("Cannot find property %s", INVOICE_DRY_RUN_TIME_PROPERTY));

        final TimeSpan span = new TimeSpan(dryRunTimePropValue);

        final DateTime now = clock.getClock().getUTCNow();
        final DateTime targetDateTime = now.plus(span.getMillis());

        final PluginCallContext callContext = new PluginCallContext(EmailNotificationActivator.PLUGIN_NAME, now, context.getTenantId());
        final Invoice invoice = osgiKillbillAPI.getInvoiceUserApi().triggerInvoiceGeneration(account.getId(), new LocalDate(targetDateTime, account.getTimeZone()), NULL_DRY_RUN_ARGUMENTS, callContext);
        if (invoice != null) {
            final EmailContent emailContent = templateRenderer.generateEmailForUpComingInvoice(account, invoice, context);
            sendEmail(account, emailContent, context);
        }
    }

    private void sendEmailForCancelledSubscription(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws SubscriptionApiException, IOException, EmailException, TenantApiException {
        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.SUBSCRIPTION_CANCEL, String.format("Unexpected event %s", killbillEvent.getEventType()));
        final UUID subscriptionId = killbillEvent.getObjectId();

        final Subscription subscription = osgiKillbillAPI.getSubscriptionApi().getSubscriptionForEntitlementId(subscriptionId, context);
        if (subscription != null && !muteEmailForObject(subscription.getBundleId(), ObjectType.BUNDLE, context)) {
            final EmailContent emailContent = subscription.getState() == Entitlement.EntitlementState.CANCELLED ?
                 templateRenderer.generateEmailForSubscriptionCancellationEffective(account, subscription, context) :
                 templateRenderer.generateEmailForSubscriptionCancellationRequested(account, subscription, context);
            sendEmail(account, emailContent, context);
        }
    }


    private void sendEmailForPayment(final Account account, final ExtBusEvent killbillEvent, final TenantContext context) throws InvoiceApiException, IOException, EmailException, PaymentApiException, TenantApiException {
        final UUID invoiceId = killbillEvent.getObjectId();
        if (invoiceId == null) {
            return;
        }

        Preconditions.checkArgument(killbillEvent.getEventType() == ExtBusEventType.INVOICE_PAYMENT_FAILED || killbillEvent.getEventType() == ExtBusEventType.INVOICE_PAYMENT_SUCCESS || killbillEvent.getEventType() == ExtBusEventType.INVOICE_CREATION,
                String.format("Unexpected event %s", killbillEvent.getEventType()));

        final Invoice invoice = osgiKillbillAPI.getInvoiceUserApi().getInvoice(invoiceId, context);

        logService.log(LogService.LOG_INFO, String.format("Payment Email request for Invoice: %s", invoiceId));

        boolean oneTimePayment = false;
        boolean recurringPayment = false;
        for (InvoiceItem current : invoice.getInvoiceItems()) {
            try {
                if (current != null && current.getInvoiceItemType() != null) {
                        if (current.getInvoiceItemType() == InvoiceItemType.EXTERNAL_CHARGE) {
                            oneTimePayment = true;
                        }
                        if (current.getInvoiceItemType() == InvoiceItemType.RECURRING) {
                            recurringPayment = true;
                        }
                }
            } catch (Exception e) {
                logService.log(LogService.LOG_WARNING, String.format("Failed to evaluate invoice items for: %s", killbillEvent.getAccountId()), e);
            }
        }

        if (killbillEvent.getEventType() == ExtBusEventType.INVOICE_CREATION) {
            // We only want to send requests for INVOICE_CREATION events that are not also generating INVOICE_PAYMENT events
            // i.e. those with no payments, and a zero balance. All others are ignored as they were before.
            // This work only effects invoices with ONLY recurring items.
            if (invoice.getNumberOfPayments() == 0) {
                if (invoice.getBalance().compareTo(BigDecimal.ZERO) == 0 && recurringPayment && !oneTimePayment) {
                    subscriptionClient.sendEmailRequest("Purchase_Success", invoice, account);
                    logService.log(LogService.LOG_INFO, String.format("Generated 0 due receipt for invoice: %s", invoiceId.toString()));
                    return;
                }
            }
            logService.log(LogService.LOG_INFO, String.format("Invoice: %s failed check with # of payments: %d, BalComp: %d, recurring: %b, and oneTime: %b",
                invoiceId.toString(), invoice.getNumberOfPayments(), invoice.getBalance().compareTo(BigDecimal.ZERO), recurringPayment, oneTimePayment));
            return;
        }

        final InvoicePayment invoicePayment = invoice.getPayments().get(invoice.getNumberOfPayments() - 1);

        final Payment payment = osgiKillbillAPI.getPaymentApi().getPayment(invoicePayment.getPaymentId(), false, false, ImmutableList.<PluginProperty>of(), context);
        final PaymentTransaction lastTransaction = payment.getTransactions().get(payment.getTransactions().size() - 1);

        if (lastTransaction.getTransactionType() != TransactionType.PURCHASE &&
                lastTransaction.getTransactionType() != TransactionType.REFUND) {
            // Ignore for now, but this is easy to add...
            return;
        }

        EmailContent emailContent = null;
        if (lastTransaction.getTransactionType() == TransactionType.REFUND && lastTransaction.getTransactionStatus() == TransactionStatus.SUCCESS) {
            emailContent = templateRenderer.generateEmailForPaymentRefund(account, lastTransaction, context);
        } else {
            if (lastTransaction.getTransactionType() == TransactionType.PURCHASE && lastTransaction.getTransactionStatus() == TransactionStatus.SUCCESS) {
                if (oneTimePayment) {
                    emailContent = templateRenderer.generateEmailForSuccessfulPayment(account, invoice, context);
                } else {
                    subscriptionClient.sendEmailRequest("Purchase_Success", invoice, account);
                    logService.log(LogService.LOG_INFO, String.format("Generated successful purchase receipt for invoice: %s", invoiceId.toString()));
                }
            } else if (lastTransaction.getTransactionType() == TransactionType.PURCHASE && lastTransaction.getTransactionStatus() == TransactionStatus.PAYMENT_FAILURE) {
                if (oneTimePayment) {
                    emailContent = templateRenderer.generateEmailForFailedPayment(account, invoice, context);
                } else {
                    subscriptionClient.sendEmailRequest("Purchase_Failure", invoice, account);
                    logService.log(LogService.LOG_INFO, String.format("Generated failed purchase receipt for invoice: %s", invoiceId.toString()));
                }
            }
        }
        if (emailContent != null) {
            sendEmail(account, emailContent, context);
            logService.log(LogService.LOG_INFO, String.format("Fell back to killbill email for Invoice: %s", invoiceId.toString()));
        }
    }

    private void sendEmail(final Account account, final EmailContent emailContent, final TenantContext context) throws IOException, EmailException {
        final Iterable<String> cc = Iterables.transform(osgiKillbillAPI.getAccountUserApi().getEmails(account.getId(), context), new Function<AccountEmail, String>() {
            @Nullable
            @Override
            public String apply(AccountEmail input) {
                return input.getEmail();
            }
        });
        emailSender.sendPlainTextEmail(ImmutableList.of(account.getEmail()), ImmutableList.copyOf(cc), emailContent.getSubject(), emailContent.getBody());
    }

    private boolean muteEmailForObject(final UUID objectId, final ObjectType objectType, final TenantContext context) {

        boolean hasMuteTag = false;
        try {
            TagDefinition muteEmailTagDefinition = osgiKillbillAPI.getTagUserApi().getTagDefinitionForName("mute_email", context);
            if (muteEmailTagDefinition == null) {
                return false;
            }

            List<Tag> tags = osgiKillbillAPI.getTagUserApi().getTagsForObject(objectId, objectType, false, context);

            for (Tag tag : tags) {
                if (muteEmailTagDefinition.getId().equals(tag.getTagDefinitionId())) {
                    hasMuteTag = true;
                }
            }
        } catch (TagDefinitionApiException e) {
            // No tag definition, so don't mute
        }
        return hasMuteTag;
    }

    private static final class EmailNotificationContext implements TenantContext {

        private final UUID tenantId;

        private EmailNotificationContext(final UUID tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public UUID getTenantId() {
            return tenantId;
        }
    }

    private final static class NullDryRunArguments implements DryRunArguments {
        @Override
        public DryRunType getDryRunType() {
            return null;
        }

        @Override
        public PlanPhaseSpecifier getPlanPhaseSpecifier() {
            return null;
        }

        @Override
        public SubscriptionEventType getAction() {
            return null;
        }

        @Override
        public UUID getSubscriptionId() {
            return null;
        }

        @Override
        public LocalDate getEffectiveDate() {
            return null;
        }

        @Override
        public UUID getBundleId() {
            return null;
        }

        @Override
        public BillingActionPolicy getBillingActionPolicy() {
            return null;
        }

        @Override
        public List<PlanPhasePriceOverride> getPlanPhasePriceOverrides() {
            return null;
        }
    }
}
