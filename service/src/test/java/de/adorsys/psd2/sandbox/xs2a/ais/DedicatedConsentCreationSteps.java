package de.adorsys.psd2.sandbox.xs2a.ais;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import de.adorsys.psd2.model.AccountAccess;
import de.adorsys.psd2.model.AccountReferenceIban;
import de.adorsys.psd2.model.ConsentInformationResponse200Json;
import de.adorsys.psd2.model.ConsentStatus;
import de.adorsys.psd2.model.Consents;
import de.adorsys.psd2.model.ConsentsResponse201;
import de.adorsys.psd2.sandbox.xs2a.SpringCucumberTestBase;
import de.adorsys.psd2.sandbox.xs2a.model.Context;
import de.adorsys.psd2.sandbox.xs2a.model.Request;
import de.adorsys.psd2.sandbox.xs2a.util.TestUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class DedicatedConsentCreationSteps extends SpringCucumberTestBase {

  private Context context = new Context();

  @Given("PSU created a consent on dedicated accounts for account information (.*), balances (.*) and transactions (.*)")
  public void psu_created_a_consent_on_dedicated_account(String accounts, String balances,
      String transactions) {

    String[] ibansForAccountAccess = accounts.split(";");
    String[] ibansForBalances = balances.split(";");
    String[] ibansForTransactions = transactions.split(";");

    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-request-id", "2f77a125-aa7a-45c0-b414-cea25a116035");
    headers.put("psu-ip-address", "192.168.0.26");
    headers.put("tpp-qwac-certificate", TestUtils.getTppQwacCertificate());

    Consents consent = new Consents();

    AccountAccess accountAccess = new AccountAccess();

    List<Object> accountList = fillAccountReferences(ibansForAccountAccess);
    List<Object> balanceList = fillAccountReferences(ibansForBalances);
    List<Object> transactionList = fillAccountReferences(ibansForTransactions);

    accountAccess.setAccounts(accountList);
    accountAccess.setBalances(balanceList);
    accountAccess.setTransactions(transactionList);

    context.setConsentAccountAccess(accountAccess);

    consent.setAccess(accountAccess);
    consent.setRecurringIndicator(true);
    consent.setValidUntil(LocalDate.now().plusDays(30));
    consent.setFrequencyPerDay(5);

    Request<Consents> request = new Request<>();
    request.setBody(consent);
    request.setHeader(headers);

    ResponseEntity<ConsentsResponse201> response = template.exchange(
        "consents",
        HttpMethod.POST,
        request.toHttpEntity(),
        ConsentsResponse201.class);

    context.setConsentId(response.getBody().getConsentId());
  }

  private List<Object> fillAccountReferences(String[] ibans) {
    ArrayList<Object> result = new ArrayList<>();

    if (ibans[0].equals("null")) {
      return null;
    }

    for (String iban : ibans) {
      AccountReferenceIban referenceIban = new AccountReferenceIban();
      referenceIban.setCurrency("EUR");
      referenceIban.setIban(iban);

      result.add(referenceIban);
    }
    return result;
  }

  @When("PSU accesses the consent data")
  public void psu_accesses_the_consent_data() {

    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-request-id", "2f77a125-aa7a-45c0-b414-cea25a116035");
    headers.put("PSU-ID", context.getPsuId());
    headers.put("tpp-qwac-certificate", TestUtils.getTppQwacCertificate());

    Request request = new Request();
    request.setHeader(headers);

    ResponseEntity<ConsentInformationResponse200Json> response = template.exchange(
        "consents/" + context.getConsentId(),
        HttpMethod.GET,
        request.toHttpEntity(),
        ConsentInformationResponse200Json.class);

    context.setActualResponse(response);
  }

  @Then("the appropriate data and response code (.*) are received")
  public void the_appropriate_data_and_response_code_are_received(String code) {

    ResponseEntity<ConsentInformationResponse200Json> actualResponse = context.getActualResponse();

    AccountAccess actualAccess = actualResponse.getBody().getAccess();
    AccountAccess expectedAccess = context.getConsentAccountAccess();

    if (expectedAccess.getAccounts() != null) {
      assertAccountReferenceIbans(actualAccess.getAccounts(), expectedAccess.getAccounts());
    }
    if (expectedAccess.getBalances() != null) {
      assertAccountReferenceIbans(actualAccess.getBalances(), expectedAccess.getBalances());
    }
    if (expectedAccess.getTransactions() != null) {
      assertAccountReferenceIbans(actualAccess.getTransactions(), expectedAccess.getTransactions());
    }

    assertThat(actualResponse.getBody().getConsentStatus(), equalTo(ConsentStatus.RECEIVED));
    assertThat(actualResponse.getStatusCodeValue(), equalTo(Integer.parseInt(code)));
    assertThat(actualResponse.getBody().getFrequencyPerDay(), equalTo(5));
    assertThat(actualResponse.getBody().getRecurringIndicator(), equalTo(true));
  }

  private void assertAccountReferenceIbans(List<Object> actualList, List<Object> expectedList) {

    LinkedHashMap<String, String> tmpActualEntry;
    HashSet<String> actualHashValues = new HashSet<>();

    for (Object entry : actualList) {
      tmpActualEntry = (LinkedHashMap<String, String>) entry;
      actualHashValues.add(tmpActualEntry.get("iban").concat(tmpActualEntry.get("currency")));
    }

    AccountReferenceIban tmpExpectedEntry;
    HashSet<String> expectedHashValues = new HashSet<>();

    for (Object entry : expectedList ) {
      tmpExpectedEntry = (AccountReferenceIban) entry;
      expectedHashValues.add(tmpExpectedEntry.getIban().concat(tmpExpectedEntry.getCurrency()));
    }

    assertThat(actualHashValues.equals(expectedHashValues), equalTo(true));
  }
}
