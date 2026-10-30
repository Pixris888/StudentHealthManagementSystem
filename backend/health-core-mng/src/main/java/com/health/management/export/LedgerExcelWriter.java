package com.health.management.export;

import com.alibaba.excel.EasyExcel;
import com.health.management.vo.report.LedgerRowVO;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 校园健康台账 Excel 生成器。
 */
@Component
public class LedgerExcelWriter {

    public void write(OutputStream outputStream, List<LedgerRowVO> rows) {
        List<LedgerExportRow> exportRows = new ArrayList<>();
        if (rows != null) {
            for (LedgerRowVO row : rows) {
                exportRows.add(ReportExportUtils.toLedgerExportRow(row));
            }
        }
        EasyExcel.write(outputStream, LedgerExportRow.class)
                .sheet("健康台账")
                .doWrite(exportRows);
    }
}
