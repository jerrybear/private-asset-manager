package com.example.assetmanager.service;

import com.example.assetmanager.domain.Account;
import com.example.assetmanager.domain.AccountType;
import com.example.assetmanager.domain.Asset;
import com.example.assetmanager.domain.AssetType;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Asset Manager Sync";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "google-credentials.json";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Sheets sheetsService;

    public GoogleSheetsService() {
    }

    // 테스트를 위한 생성자
    public GoogleSheetsService(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    @Value("${google.sheet.id}")
    private String spreadsheetId;

    /**
     * 시트에서 계좌 정보와 포함된 자산 목록을 가져옵니다.
     */
    public Account fetchAccountFromSheet(String sheetName)
            throws IOException, GeneralSecurityException {
        Sheets service = getSheetsServiceInstance();
        String targetSheetName = sheetName;

        if (targetSheetName == null || targetSheetName.isBlank()) {
            com.google.api.services.sheets.v4.model.Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                    .execute();
            targetSheetName = spreadsheet.getSheets().get(0).getProperties().getTitle();
        }

        // 1. 계좌 정보 읽기 (1~2행)
        // A1:F2 범위 (계좌명, 소유자, 계좌종류, 금융기관, 계좌번호, 설명)
        String accountRange = "'" + targetSheetName + "'!A1:F2";
        log.info("Fetching account metadata from sheet: {}, range: {}", targetSheetName, accountRange);
        ValueRange accountResponse = service.spreadsheets().values()
                .get(spreadsheetId, accountRange)
                .execute();
        List<List<Object>> accountValues = accountResponse.getValues();

        Account account = new Account();
        account.setSheetName(targetSheetName);

        if (accountValues != null && accountValues.size() >= 2) {
            List<Object> dataRow = accountValues.get(1); // 2행이 실제 데이터
            log.info("Account data row: {}", dataRow);
            if (dataRow.size() >= 1)
                account.setName(dataRow.get(0).toString().trim());
            if (dataRow.size() >= 2)
                account.setOwner(dataRow.get(1).toString().trim());
            if (dataRow.size() >= 3) {
                account.setAccountType(parseAccountType(dataRow.get(2).toString()));
            }
            if (dataRow.size() >= 4)
                account.setFinancialInstitution(dataRow.get(3).toString().trim());
            if (dataRow.size() >= 5)
                account.setAccountNumber(dataRow.get(4).toString().trim());
            if (dataRow.size() >= 6)
                account.setDescription(dataRow.get(5).toString().trim());
        } else {
            log.warn("Account metadata not found in sheet: {}. accountValues: {}", targetSheetName, accountValues);
        }

        // 2. 자산 목록 읽기 (4행부터)
        // A:종목코드, B:종목명, C:수량, D:평균단가, E:유형, F:현재가, G:가격조회일자, H:배당주기, I:1회당배당금
        String assetRange = "'" + targetSheetName + "'!A5:I"; // A5부터 데이터 (4행은 헤더)
        log.info("Fetching asset list from sheet: {}, range: {}", targetSheetName, assetRange);
        ValueRange assetResponse = service.spreadsheets().values()
                .get(spreadsheetId, assetRange)
                .execute();
        List<List<Object>> assetValues = assetResponse.getValues();

        List<Asset> assets = new ArrayList<>();
        if (assetValues != null) {
            log.info("Found {} asset rows in sheet: {}", assetValues.size(), targetSheetName);
            for (List<Object> row : assetValues) {
                try {
                    if (row.isEmpty())
                        continue;
                    if (row.size() < 4) {
                        log.warn("Skipping incomplete asset row: {}", row);
                        continue;
                    }

                    String code = row.get(0).toString().trim();
                    String name = row.get(1).toString().trim();
                    BigDecimal quantity = new BigDecimal(row.get(2).toString().trim().replace(",", ""));
                    BigDecimal averagePrice = new BigDecimal(row.get(3).toString().trim().replace(",", ""));

                    AssetType type = AssetType.STOCK;
                    if (row.size() >= 5) {
                        type = parseAssetType(row.get(4).toString());
                    }

                    BigDecimal currentPrice = null;
                    if (row.size() >= 6) {
                        String priceStr = row.get(5).toString().trim().replace(",", "");
                        if (!priceStr.isBlank()) {
                            currentPrice = new BigDecimal(priceStr);
                        }
                    }

                    java.time.LocalDateTime lastPriceUpdate = null;
                    if (row.size() >= 7) {
                        String updateDateStr = row.get(6).toString().trim();
                        if (!updateDateStr.isBlank()) {
                            try {
                                lastPriceUpdate = java.time.LocalDateTime.parse(updateDateStr, DATE_TIME_FORMATTER);
                            } catch (Exception e) {
                                try {
                                    // 기존 ISO 포맷도 하위 호환성을 위해 시도
                                    lastPriceUpdate = java.time.LocalDateTime.parse(updateDateStr);
                                } catch (Exception e2) {
                                    log.warn("Failed to parse lastPriceUpdate with any format: {}. Error: {}",
                                            updateDateStr, e2.getMessage());
                                }
                            }
                        }
                    }

                    String dividendCycle = "없음";
                    if (row.size() >= 8) {
                        dividendCycle = row.get(7).toString().trim();
                    }

                    BigDecimal dividendPerShare = BigDecimal.ZERO;
                    if (row.size() >= 9) {
                        String divStr = row.get(8).toString().trim().replace(",", "");
                        if (!divStr.isBlank()) {
                            dividendPerShare = new BigDecimal(divStr);
                        }
                    }

                    assets.add(new Asset(account, type, code, name, quantity, averagePrice, currentPrice,
                            lastPriceUpdate, LocalDate.now(), dividendCycle, dividendPerShare));
                } catch (Exception e) {
                    log.error("Failed to parse asset row: {}. Error: {}", row, e.getMessage());
                }
            }
        }
        account.setAssets(assets);
        log.info("Finished fetching account data. Total assets: {}", assets.size());

        return account;
    }

    /**
     * 계좌 정보와 자산 목록을 시트에 업데이트합니다.
     */
    public void updateSheetWithAccount(Account account)
            throws IOException, GeneralSecurityException {
        Sheets service = getSheetsServiceInstance();
        String targetSheetName = account.getSheetName();

        if (targetSheetName == null || targetSheetName.isBlank()) {
            throw new IllegalArgumentException("Sheet name must not be blank");
        }

        // 시트 ID 가져오기
        com.google.api.services.sheets.v4.model.Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .execute();
        Integer sheetId = spreadsheet.getSheets().stream()
                .filter(s -> s.getProperties().getTitle().equals(targetSheetName))
                .map(s -> s.getProperties().getSheetId())
                .findFirst()
                .orElseThrow(() -> new IOException("Sheet not found: " + targetSheetName));

        // 1. 데이터 준비
        List<RowData> rows = new ArrayList<>();

        // 1행: 계좌 헤더
        rows.add(createHeaderRow(Arrays.asList("계좌명", "소유자", "계좌종류", "금융기관", "계좌번호", "설명")));
        // 2행: 계좌 데이터
        rows.add(createDataRow(Arrays.asList(
                account.getName(), account.getOwner(),
                account.getAccountType() != null ? account.getAccountType().getDescription() : "",
                account.getFinancialInstitution(), account.getAccountNumber(), account.getDescription())));
        // 3행: 빈 줄
        rows.add(new RowData());
        // 4행: 자산 헤더
        rows.add(createHeaderRow(
                Arrays.asList("종목코드", "종목명", "수량", "평균단가", "유형", "현재가", "가격조회일자", "배당주기", "1회당 배당금")));

        // 5행부터: 자산 데이터
        for (Asset asset : account.getAssets()) {
            List<Object> values = Arrays.asList(
                    asset.getCode(),
                    asset.getName(),
                    asset.getQuantity() != null ? asset.getQuantity() : BigDecimal.ZERO, // Numeric
                    asset.getAveragePurchasePrice() != null ? asset.getAveragePurchasePrice() : BigDecimal.ZERO, // Numeric
                    asset.getType() != null ? asset.getType().getDescription() : "",
                    asset.getCurrentPrice() != null ? asset.getCurrentPrice() : BigDecimal.ZERO, // Numeric
                    asset.getLastPriceUpdate() != null ? asset.getLastPriceUpdate().format(DATE_TIME_FORMATTER) : "",
                    asset.getDividendCycle() != null ? asset.getDividendCycle() : "없음",
                    asset.getDividendPerShare() != null ? asset.getDividendPerShare() : BigDecimal.ZERO // Numeric
            );
            rows.add(createMixedDataRow(values, Arrays.asList(2, 3, 5, 8))); // 2,3,5,8번 인덱스는 숫자
        }

        List<Request> requests = new ArrayList<>();

        // 기존 데이터 삭제 및 새 데이터 쓰기
        requests.add(new Request().setUpdateCells(new UpdateCellsRequest()
                .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(0).setStartColumnIndex(0))
                .setRows(rows)
                .setFields("userEnteredValue,userEnteredFormat")));

        // 열 너비 자동 조정 및 명시적 설정
        requests.add(new Request().setAutoResizeDimensions(new AutoResizeDimensionsRequest()
                .setDimensions(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(0)
                        .setEndIndex(10)))); // 전체적으로 자동 조정 시도

        // 특정 열들에 대해 보기 좋은 너비로 명시적 설정 (A=0, B=1, ...)
        requests.add(createUpdateDimensionWidthRequest(sheetId, 1, 220)); // B: 종목명
        requests.add(createUpdateDimensionWidthRequest(sheetId, 3, 150)); // D: 금융기관 / 평균단가
        requests.add(createUpdateDimensionWidthRequest(sheetId, 4, 120)); // E: 유형 / 계좌번호
        requests.add(createUpdateDimensionWidthRequest(sheetId, 6, 180)); // G: 가격조회일자

        // 드롭다운 설정 (유형 열: E열, Index 4)
        ConditionValue[] assetTypeValues = Arrays.stream(AssetType.values())
                .map(type -> new ConditionValue().setUserEnteredValue(type.getDescription()))
                .toArray(ConditionValue[]::new);

        requests.add(new Request().setSetDataValidation(new SetDataValidationRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(4) // 5행부터
                        .setStartColumnIndex(4)
                        .setEndColumnIndex(5))
                .setRule(new DataValidationRule()
                        .setCondition(new BooleanCondition()
                                .setType("ONE_OF_LIST")
                                .setValues(Arrays.asList(assetTypeValues)))
                        .setShowCustomUi(true))));

        // 배당주기 드롭다운 (H열, Index 7)
        ConditionValue[] dividendCycleValues = Arrays.asList("1개월", "3개월", "6개월", "12개월", "없음").stream()
                .map(v -> new ConditionValue().setUserEnteredValue(v))
                .toArray(ConditionValue[]::new);

        requests.add(new Request().setSetDataValidation(new SetDataValidationRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(4) // 5행부터
                        .setStartColumnIndex(7)
                        .setEndColumnIndex(8))
                .setRule(new DataValidationRule()
                        .setCondition(new BooleanCondition()
                                .setType("ONE_OF_LIST")
                                .setValues(Arrays.asList(dividendCycleValues)))
                        .setShowCustomUi(true))));

        // 계좌종류 드롭다운 (C2: Index row 1, col 2)
        ConditionValue[] accountTypeValues = Arrays.stream(AccountType.values())
                .map(type -> new ConditionValue().setUserEnteredValue(type.getDescription()))
                .toArray(ConditionValue[]::new);

        requests.add(new Request().setSetDataValidation(new SetDataValidationRequest()
                .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(1)
                        .setEndRowIndex(2)
                        .setStartColumnIndex(2)
                        .setEndColumnIndex(3))
                .setRule(new DataValidationRule()
                        .setCondition(new BooleanCondition()
                                .setType("ONE_OF_LIST")
                                .setValues(Arrays.asList(accountTypeValues)))
                        .setShowCustomUi(true))));

        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        service.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute();

        log.info("Successfully updated Google Sheet with styles: {}, Sheet Name: {}", spreadsheetId, targetSheetName);
    }

    private RowData createHeaderRow(List<String> values) {
        List<CellData> cells = values.stream().map(v -> new CellData()
                .setUserEnteredValue(new ExtendedValue().setStringValue(v))
                .setUserEnteredFormat(new CellFormat()
                        .setBackgroundColor(new Color().setRed(0.9f).setGreen(0.9f).setBlue(0.9f))
                        .setTextFormat(new TextFormat().setBold(true))
                        .setBorders(createThinBorders())))
                .collect(Collectors.toList());
        return new RowData().setValues(cells);
    }

    private RowData createDataRow(List<String> values) {
        List<CellData> cells = values.stream().map(v -> new CellData()
                .setUserEnteredValue(new ExtendedValue().setStringValue(v != null ? v : ""))
                .setUserEnteredFormat(new CellFormat().setBorders(createThinBorders())))
                .collect(Collectors.toList());
        return new RowData().setValues(cells);
    }

    private RowData createMixedDataRow(List<Object> values, List<Integer> numericIndices) {
        List<CellData> cells = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Object v = values.get(i);
            CellData cell = new CellData().setUserEnteredFormat(new CellFormat().setBorders(createThinBorders()));

            if (numericIndices.contains(i) && v instanceof BigDecimal) {
                cell.setUserEnteredValue(new ExtendedValue().setNumberValue(((BigDecimal) v).doubleValue()));
                cell.getUserEnteredFormat().setNumberFormat(new NumberFormat().setType("NUMBER").setPattern("#,##0"));
            } else {
                cell.setUserEnteredValue(new ExtendedValue().setStringValue(v != null ? v.toString() : ""));
            }
            cells.add(cell);
        }
        return new RowData().setValues(cells);
    }

    private Borders createThinBorders() {
        Border thinSolid = new Border().setStyle("SOLID")
                .setColor(new Color().setRed(0.0f).setGreen(0.0f).setBlue(0.0f));
        return new Borders()
                .setTop(thinSolid).setBottom(thinSolid)
                .setLeft(thinSolid).setRight(thinSolid);
    }

    private Request createUpdateDimensionWidthRequest(Integer sheetId, int colIndex, int width) {
        return new Request().setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(colIndex)
                        .setEndIndex(colIndex + 1))
                .setProperties(new DimensionProperties().setPixelSize(width))
                .setFields("pixelSize"));
    }

    public List<String> getSheetNames() throws IOException, GeneralSecurityException {
        Sheets service = getSheetsServiceInstance();
        com.google.api.services.sheets.v4.model.Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId)
                .execute();

        return spreadsheet.getSheets().stream()
                .map(sheet -> sheet.getProperties().getTitle())
                .collect(Collectors.toList());
    }

    private AccountType parseAccountType(String value) {
        if (value == null || value.isBlank())
            return AccountType.REGULAR;
        String v = value.toUpperCase().trim();

        // 1. Enum name()과 일치하는지 확인
        for (AccountType type : AccountType.values()) {
            if (type.name().equals(v))
                return type;
        }
        // 2. description과 일치하는지 확인
        for (AccountType type : AccountType.values()) {
            if (type.getDescription().equalsIgnoreCase(v))
                return type;
        }

        // 3. 기존의 유연한 매칭
        if (v.contains("주식") || v.contains("STOCK") || v.contains("일반"))
            return AccountType.REGULAR;
        if (v.contains("연금") || v.contains("PENSION"))
            return AccountType.PENSION;
        if (v.contains("ISA"))
            return AccountType.ISA;
        if (v.contains("IRP"))
            return AccountType.IRP;
        return AccountType.SPECIAL;
    }

    private AssetType parseAssetType(String value) {
        if (value == null || value.isBlank())
            return AssetType.STOCK;
        String v = value.toUpperCase().trim();

        // 1. Enum name()과 일치하는지 확인
        for (AssetType type : AssetType.values()) {
            if (type.name().equals(v))
                return type;
        }
        // 2. description과 일치하는지 확인
        for (AssetType type : AssetType.values()) {
            if (type.getDescription().equalsIgnoreCase(v))
                return type;
        }

        // 3. 기존의 유연한 매칭
        if (v.contains("STOCK_KR") || v.contains("국내주식") || v.contains("KOREA STOCK"))
            return AssetType.STOCK_KR;
        if (v.contains("STOCK_US") || v.contains("해외주식") || v.contains("US STOCK") || v.contains("미국주식"))
            return AssetType.STOCK_US;
        if (v.contains("STOCK") || v.contains("주식"))
            return AssetType.STOCK;

        if (v.contains("CRYPTO") || v.contains("코인") || v.contains("가상") || v.contains("화폐"))
            return AssetType.CRYPTO;
        if (v.contains("CASH") || v.contains("현금"))
            return AssetType.CASH;
        if (v.contains("RP"))
            return AssetType.RP;
        if (v.contains("ISSUED") || v.contains("발행어음"))
            return AssetType.ISSUED_NOTE;

        if (v.contains("BOND_KR") || v.contains("국내채권"))
            return AssetType.BOND_KR;
        if (v.contains("BOND_US") || v.contains("해외채권") || v.contains("미국채권"))
            return AssetType.BOND_US;
        if (v.contains("BOND") || v.contains("채권"))
            return AssetType.BOND;

        if (v.contains("REITS") || v.contains("리츠") || v.contains("부동산"))
            return AssetType.REITS;
        if (v.contains("COMMODITY") || v.contains("원자재"))
            return AssetType.COMMODITY;

        if (v.contains("DEPOSIT") || v.contains("SAVING") || v.contains("예적금") || v.contains("예금") || v.contains("적금"))
            return AssetType.DEPOSIT_SAVINGS;
        if (v.contains("GOLD") || v.contains("금현물") || v.contains("금"))
            return AssetType.GOLD_SPOT;
        return AssetType.STOCK;
    }

    private synchronized Sheets getSheetsServiceInstance() throws IOException, GeneralSecurityException {
        if (this.sheetsService != null) {
            return this.sheetsService;
        }
        this.sheetsService = createSheetsService();
        return this.sheetsService;
    }

    private Sheets createSheetsService() throws IOException, GeneralSecurityException {
        ClassPathResource resource = new ClassPathResource(CREDENTIALS_FILE_PATH);
        if (!resource.exists()) {
            throw new IOException("Credentials file not found in classpath: " + CREDENTIALS_FILE_PATH);
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
