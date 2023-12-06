package tianjhenhaokevin.valueStocksScreenerV2;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataAPI {
    private static final String LOGIN_EMAIL = "<Login Email here>";
    private static final String LOGIN_PW = "<Login Password here>";
    private static final String LOGIN_PAGE_URL = "<Private Data Endpoint URL>";

    private WebClient webClient = null;

    private HtmlPage toolsPage = null;

    public void init() throws IOException {
        this.webClient = new WebClient();
        this.webClient.getOptions().setCssEnabled(false);

        HtmlPage loginPage = webClient.getPage(LOGIN_PAGE_URL);

        HtmlAnchor loginFormAnchor = loginPage.getAnchorByHref("#login-form");
        HtmlPage loginPage2 = loginFormAnchor.click();
        HtmlForm loginForm2 = loginPage2.getFormByName("loginform");

        HtmlInput usernameInput = loginForm2.getInputByName("log");
        HtmlInput pwInput = loginForm2.getInputByName("pwd");
        HtmlSubmitInput loginButton = loginForm2.getInputByName("wp-submit");
        usernameInput.type(LOGIN_EMAIL);
        pwInput.type(LOGIN_PW);

        toolsPage = loginButton.click();

        HtmlButton fundamentalDataButton = toolsPage.getFirstByXPath("//button[text()=\"Fundamental Data\"]");
        toolsPage = fundamentalDataButton.click();
    }

    public void close() {
        try {
            this.toolsPage.cleanUp();
            this.webClient.close();
        } catch (Exception e) {}

        this.toolsPage = null;
        this.webClient = null;
    }

    public Map<String, Object> queryData(String ticker) throws Exception {
        HtmlForm fetchForm = toolsPage.getFirstByXPath("//form[@class='fetchform']");
        HtmlInput tickerInput = fetchForm.getInputByName("stock");
        HtmlButton submitButton = fetchForm.getFirstByXPath("button");

        tickerInput.type(ticker);
        this.toolsPage = submitButton.click();

        //Extracting year data
        HtmlTableRow yearTableRow = this.toolsPage.getFirstByXPath("//tr[contains(@class, 'tr-3 alhead')]");
        String lastColumn = yearTableRow.getCells().get(10).getTextContent().replaceAll("[\\n\\t]", "");

        //Extracting Revenue data
        HtmlTableRow revenueTableRow = this.toolsPage.getFirstByXPath("//tr[contains(@class, 'tr-4')]");
        List<HtmlTableCell> revenueTableRowCells = revenueTableRow.getCells();

        String revenueRowLabel = revenueTableRowCells.get(0).getTextContent();

        if(revenueTableRowCells.size() != 12 || !revenueRowLabel.contains("Revenue")) {
            throw new Exception("Invalid [Revenue] data row format");
        }

        List<String> revenueRowLabelComponents = Stream.of(revenueRowLabel.split(" "))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        if(revenueRowLabelComponents.size() != 3 || revenueRowLabelComponents.get(1).length()  != 3) {
            throw new Exception("Invalid currency code in [Revenue] label");
        }

        int columnStartIndex = -1;

        for(int i=1; i<11; i++) {
            double revenueData = Double.parseDouble(
                    revenueTableRowCells.get(i).getTextContent().replace(",", ""));

            if(revenueData != 0) {
                columnStartIndex = i;
                break;
            }
        }

        if(columnStartIndex == -1) {
            throw new Exception("All [Revenue] data points are zero");
        }

        //Extracting EPS data
        HtmlTableRow epsTableRow = this.toolsPage.getFirstByXPath("//tr[contains(@class, 'tr-9')]");
        List<HtmlTableCell> epsTableRowCells = epsTableRow.getCells();

        if(epsTableRowCells.size() != 12 || !epsTableRowCells.get(0).getTextContent().contains("Earnings Per Share")) {
            throw new Exception("Invalid [Earnings Per Share] data row format");
        }

        List<Double> epsDataList = new ArrayList<>();

        for (int i = columnStartIndex; i<11; i++) {
            double epsData = Double.parseDouble(
                    epsTableRowCells.get(i).getTextContent().replace(",", ""));
            epsDataList.add(epsData);
        }

        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("currency", revenueRowLabelComponents.get(1));
        dataMap.put("last_eps_year", lastColumn);
        dataMap.put("eps", epsDataList);

        return dataMap;
    }

    public static void main(String[] args) throws Exception {
        // Testing
        DataAPI dataAPI = new DataAPI();
        dataAPI.init();
        Map<String, Object> dataMap = dataAPI.queryData("INMD");
        System.out.println(dataMap);
    }
}