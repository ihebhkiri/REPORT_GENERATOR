package RHIS.com.RHIS.dataset.controller;

import RHIS.com.RHIS.dataset.controller.dto.DataSetFieldResponse;
import RHIS.com.RHIS.dataset.controller.dto.DataSetResponse;
import RHIS.com.RHIS.dataset.controller.dto.TableRelationResponse;
import RHIS.com.RHIS.dataset.service.DataSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/datasets")
@RequiredArgsConstructor
public class DataSetController {
    private final DataSetService dataSetsService;

    @GetMapping
    public ResponseEntity<List<DataSetResponse>> getDatasets() {
        return ResponseEntity.ok(dataSetsService.getDataSets());
    }

    @GetMapping("/relations")
    public ResponseEntity<List<TableRelationResponse>> getRelations() {
        return ResponseEntity.ok(dataSetsService.getRelations());
    }

    @GetMapping("/{dataSetId}/fields")
    public ResponseEntity<List<DataSetFieldResponse>> getDataSetsFieldsByDataSetId(@PathVariable Long dataSetId) {
        return ResponseEntity.ok(dataSetsService.getDataSetFields(dataSetId));
    }

}
