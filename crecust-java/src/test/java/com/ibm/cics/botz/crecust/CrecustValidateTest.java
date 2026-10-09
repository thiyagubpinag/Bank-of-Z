package com.ibm.cics.botz.crecust;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.ibm.cics.botz.crecust.model.CrecustCommarea;
import com.ibm.cics.botz.crecust.model.WsChildData;
import com.ibm.cics.botz.crecust.db.HostCustomerRow;
import com.ibm.cics.botz.crecust.model.CustomerRecord;
import com.ibm.cics.botz.crecust.serializer.CrecustareaSerializer;
import com.ibm.cics.botz.crecust.serializer.WsChildDataSerializer;
import com.ibm.cics.botz.crecust.service.AbndprocDelegate;
import com.ibm.cics.botz.crecust.service.CustomerDbService;
import com.ibm.cics.botz.crecust.service.ProctranDbService;
import com.ibm.cics.botz.crecust.service.ChildResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrecustValidateTest {

    @Mock
    private AbndprocDelegate mockAbndprocDelegate;

    @Mock
    private CustomerDbService mockCustomerDbService;

    @Mock
    private ProctranDbService mockProctranDbService;

    private MockedStatic<AbndprocDelegate> staticAbndprocDelegate;
    private MockedStatic<CustomerDbService> staticCustomerDbService;
    private MockedStatic<ProctranDbService> staticProctranDbService;

    @BeforeEach
    void setUp() {
        staticAbndprocDelegate = mockStatic(AbndprocDelegate.class);
        staticAbndprocDelegate.when(AbndprocDelegate::getInstance).thenReturn(mockAbndprocDelegate);

        staticCustomerDbService = mockStatic(CustomerDbService.class);
        staticCustomerDbService.when(CustomerDbService::getInstance).thenReturn(mockCustomerDbService);

        staticProctranDbService = mockStatic(ProctranDbService.class);
        staticProctranDbService.when(ProctranDbService::getInstance).thenReturn(mockProctranDbService);
    }

    @AfterEach
    void tearDown() {
        if (staticAbndprocDelegate != null) {
            staticAbndprocDelegate.close();
        }
        if (staticCustomerDbService != null) {
            staticCustomerDbService.close();
        }
        if (staticProctranDbService != null) {
            staticProctranDbService.close();
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Validate CustomerNumberSequenceGeneration — Crecust")
    public void testCrecust_CustomerNumberSequenceGeneration() throws Exception {
        // Coverage: 2 test points — verified 2, partial 0, unverifiable 0, skipped 0
        // Assertion failures indicate genuine transformation divergence (Type C).
        // Unverifiable counts reflect observability gaps; skipped counts reflect missing/zero-confidence mappings — neither are test failures.

        // ===== SETUP INITIAL STATE =====
        CrecustCommarea inputCommarea = CrecustCommarea.builder()
                .commSortcode("987654")
                .commTitle("Mr        ")
                .commFirstName("BANKZCUST")
                .commLastName("TESTER")
                .commDobDay("20")
                .commDobMonth("10")
                .commDobYear("1980")
                .commPhone("12345678")
                .commAddrLine1("Address 1")
                .commAddrLine2("Address 2")
                .commCity("City")
                .commPostcode("12345")
                .commCountry("Country")
                .commStatus("Active")
                .commCreatedDay("01")
                .commCreatedMonth("01")
                .commCreatedYear("2020")
                .commCreditScore("750")
                .commSuccess("Y")
                .commFailCode(" ")
                .build();

        byte[] cah = CrecustareaSerializer.INSTANCE.toBytes(new byte[CrecustareaSerializer.SIZE], 0, inputCommarea);

        // ===== CONFIGURE MOCKS =====
        // Configure AbndprocDelegate stubs
        when(mockAbndprocDelegate.readCommarea(any(byte[].class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(mockAbndprocDelegate.runTransactionId(anyString(), anyString()))
                .thenReturn(0)
                .thenReturn(1)
                .thenReturn(2)
                .thenReturn(3)
                .thenReturn(4);

        when(mockAbndprocDelegate.fetchAnyNosuspend())
                .thenReturn(new ChildResult(ChildResult.STATUS_NORMAL, 0))
                .thenReturn(new ChildResult(ChildResult.STATUS_NORMAL, 1))
                .thenReturn(new ChildResult(ChildResult.STATUS_NORMAL, 2))
                .thenReturn(new ChildResult(ChildResult.STATUS_NORMAL, 3))
                .thenReturn(new ChildResult(ChildResult.STATUS_NORMAL, 4));

        when(mockAbndprocDelegate.getTaskNumber()).thenReturn(12345L);

        WsChildData childData = WsChildData.builder()
                .customerCreditScore("100")
                .wsChildSuccess("Y")
                .build();
        byte[] containerBytes = WsChildDataSerializer.INSTANCE.toBytes(new byte[WsChildDataSerializer.SIZE], 0, childData);
        when(mockAbndprocDelegate.getContainerBytes(anyInt(), anyString())).thenReturn(containerBytes);

        // Configure CustomerDbService stubs
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockSelectStatement = mock(PreparedStatement.class);
        PreparedStatement mockUpdateStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockCustomerDbService.getControlConnection()).thenReturn(mockConnection);
        when(mockCustomerDbService.prepareControlSelect(any(Connection.class), anyString())).thenReturn(mockSelectStatement);
        when(mockCustomerDbService.executeControlSelect(any(PreparedStatement.class))).thenReturn(mockResultSet);
        when(mockCustomerDbService.controlResultSetNext(any(ResultSet.class))).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt(1)).thenReturn(100);

        when(mockCustomerDbService.prepareControlUpdate(any(Connection.class), anyString())).thenReturn(mockUpdateStatement);

        doAnswer(invocation -> {
            CrecustCommarea comm = invocation.getArgument(0);
            comm.setCommSuccess("Y");
            comm.setCommFailCode(" ");
            comm.setCommEyecatcher("CUST");
            return null;
        }).when(mockCustomerDbService).insertCustomer(
                any(CrecustCommarea.class),
                any(CustomerRecord.class),
                any(HostCustomerRow.class),
                any(),
                anyString()
        );

        // ===== EXECUTE PROGRAM =====
        Crecust.main(cah);
        // ===== VERIFY FINAL STATE =====
        // test_point_id: CRECUST_OP_001  from_external_system: CONTROL_VALUE_NUM = 100  [confidence_score: 0.95]
        verify(mockCustomerDbService).executeControlSelect(mockSelectStatement);
        verify(mockSelectStatement).setString(1, "BANKZCUST987654                 ");

        // test_point_id: CRECUST_OP_002  to_external_system: CONTROL_VALUE_NUM = 101, CONTROL_NAME = BANKZCUST987654                 
        verify(mockCustomerDbService).executeControlUpdate(mockUpdateStatement);
        verify(mockUpdateStatement).setInt(1, 101);
        verify(mockUpdateStatement).setString(2, "BANKZCUST987654                 ");

        // Assert Final Program-Level Outputs (Rule V0 / End-to-End Contract)
        ArgumentCaptor<byte[]> outCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(mockAbndprocDelegate).writeCommarea(outCaptor.capture(), any(byte[].class));
        byte[] resultBytes = outCaptor.getValue();
        CrecustCommarea resultCommarea = CrecustareaSerializer.INSTANCE.fromBytes(resultBytes, 0);

        // COMM-NUMBER = 0000000101
        assertEquals("0000000101", resultCommarea.getCommNumber(), "COMM-NUMBER");

        // CUSTOMER-NUMBER = 0000000101
        // insertCustomer is mocked, so HostCustomerRow is never populated (the real implementation derives
        // hvCustomerNumber from commArea.getCommNumber()). Observe the number handed to insertCustomer instead.
        ArgumentCaptor<CrecustCommarea> insertCommareaCaptor = ArgumentCaptor.forClass(CrecustCommarea.class);
        verify(mockCustomerDbService).insertCustomer(
                insertCommareaCaptor.capture(),
                any(CustomerRecord.class),
                any(HostCustomerRow.class),
                any(),
                anyString()
        );
        assertEquals("0000000101", insertCommareaCaptor.getValue().getCommNumber(), "CUSTOMER-NUMBER");

        // UNVERIFIABLE — REQUIRED-CUST-NUMBER2 (mock_variable: null): local CustomerKy2 variable not observable
        // UNVERIFIABLE — NCS-CUST-NO-VALUE (mock_variable: null): local NcsCustNoStuff variable not observable

        // Verify ProctranDbService insert details as part of integration trace
        verify(mockProctranDbService).insertProctran(
                any(CrecustCommarea.class),
                eq("987654"),
                eq("0000000101"),
                eq("BANKZCUST TESTER"),
                eq("20/10/1980"),
                any(),
                any(),
                any(AbndprocDelegate.class)
        );
    }
}
