package RHIS.com.RHIS.dataset.service;

import RHIS.com.RHIS.GenericCrudService;
import RHIS.com.RHIS.dataset.controller.dto.DataSetFieldResponse;
import RHIS.com.RHIS.dataset.controller.dto.DataSetResponse;
import RHIS.com.RHIS.dataset.controller.dto.JoinInfoProjection;
import RHIS.com.RHIS.dataset.controller.dto.TableRelationResponse;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;

import java.util.List;

public interface DataSetService extends GenericCrudService<DataSetEntity, Long> {
    List<DataSetResponse> getDataSets();

    List<DataSetFieldResponse> getDataSetFields(Long dataSetId);


    List<TableRelationResponse> getRelations();
}
