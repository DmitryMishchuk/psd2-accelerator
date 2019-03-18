Feature: Payment Instrument Issuer Service

    ################################################################################################
    #                                                                                              #
    # Confirmation of funds                                                                        #
    #                                                                                              #
    ################################################################################################
  Scenario Outline: Successful confirmation of funds
    Given PSU wants to check the availability of funds
    When PSU sends the request with the amount <amount> and the IBAN <iban>
    Then the status <availability-status> and response code <code> are received
    Examples:
      | amount | iban                   | availability-status | code |
      | 500    | DE11760365688833114935 | true                | 200  |
      | 2500   | DE11760365688833114935 | false               | 200  |
      | 500    | AT022050302101023600   | false               | 200  |
      | 2500   | AT022050302101023600   | false               | 200  |

  Scenario Outline: Confirmation of funds request with wrong currency
    Given PSU tries to check the availability of funds with the amount <amount> and the IBAN <iban> and currency <currency>
    Then an error-message <error-message> is received
    Examples:
      | amount | iban                   | currency | error-message |
      | 500    | DE11760365688833114935 | USD      | FORMAT_ERROR  |
      | 500    | DE56760365681650680255 | EUR      | FORMAT_ERROR  |
