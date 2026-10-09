package com.ibm.cics.botz.crecust.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Concrete view for {@code PROC-TRAN-DESC-CRECUS} over the 40-byte shared buffer (TRA-7).
 *
 * <p>Layout:
 * <ul>
 *   <li>sortCode: offset 0, len 6</li>
 *   <li>custNo: offset 6, len 10</li>
 *   <li>name: offset 16, len 14</li>
 *   <li>dob: offset 30, len 10</li>
 * </ul>
 *
 * <p>Declares NO instance fields. All getters and setters encode/decode directly against
 * {@link ProcTranDescBase#getData()}.
 */
public class ProcTranDescCrecus extends ProcTranDescBase {

    public ProcTranDescCrecus(byte[] data) {
        super(data);
    }

    public String getSortCode() {
        return readString(ProcTranDescConstants.CRECUS_SORT_CODE_OFFSET, ProcTranDescConstants.CRECUS_SORT_CODE_LEN);
    }

    public void setSortCode(String sortCode) {
        writeString(sortCode, ProcTranDescConstants.CRECUS_SORT_CODE_OFFSET, ProcTranDescConstants.CRECUS_SORT_CODE_LEN);
    }

    public String getCustNo() {
        return readString(ProcTranDescConstants.CRECUS_CUST_NO_OFFSET, ProcTranDescConstants.CRECUS_CUST_NO_LEN);
    }

    public void setCustNo(String custNo) {
        writeString(custNo, ProcTranDescConstants.CRECUS_CUST_NO_OFFSET, ProcTranDescConstants.CRECUS_CUST_NO_LEN);
    }

    public String getName() {
        return readString(ProcTranDescConstants.CRECUS_NAME_OFFSET, ProcTranDescConstants.CRECUS_NAME_LEN);
    }

    public void setName(String name) {
        writeString(name, ProcTranDescConstants.CRECUS_NAME_OFFSET, ProcTranDescConstants.CRECUS_NAME_LEN);
    }

    public String getDob() {
        return readString(ProcTranDescConstants.CRECUS_DOB_OFFSET, ProcTranDescConstants.CRECUS_DOB_LEN);
    }

    public void setDob(String dob) {
        writeString(dob, ProcTranDescConstants.CRECUS_DOB_OFFSET, ProcTranDescConstants.CRECUS_DOB_LEN);
    }

    private String readString(int offset, int length) {
        byte[] data = getData();
        return new String(data, offset, length, StandardCharsets.ISO_8859_1).trim();
    }

    private void writeString(String value, int offset, int length) {
        byte[] data = getData();
        Arrays.fill(data, offset, offset + length, (byte) ' ');
        if (value != null && !value.isEmpty()) {
            byte[] encoded = value.getBytes(StandardCharsets.ISO_8859_1);
            int copyLen = Math.min(encoded.length, length);
            System.arraycopy(encoded, 0, data, offset, copyLen);
        }
    }
}
