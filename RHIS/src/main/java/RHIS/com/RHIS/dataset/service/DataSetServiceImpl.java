package RHIS.com.RHIS.dataset.service;


import RHIS.com.RHIS.GenericCrudServiceImpl;
import RHIS.com.RHIS.dataset.controller.DataSetMapper;
import RHIS.com.RHIS.dataset.controller.dto.DataSetFieldResponse;
import RHIS.com.RHIS.dataset.controller.dto.DataSetResponse;
import RHIS.com.RHIS.dataset.controller.dto.TableRelationResponse;
import RHIS.com.RHIS.dataset.entity.DataSetEntity;
import RHIS.com.RHIS.dataset.repository.DataSetFieldRepository;
import RHIS.com.RHIS.dataset.repository.DataSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class DataSetServiceImpl extends GenericCrudServiceImpl<DataSetEntity, Long> implements DataSetService {
    private final DataSetRepository dataSetRepository;
    private final DataSetFieldRepository dataSetFieldRepository;
    private final DataSetMapper dataSetMapper;

    public DataSetServiceImpl(
            DataSetRepository repository,
            DataSetFieldRepository dataSetFieldRepository,
            DataSetMapper dataSetMapper
    ) {
        super(repository, "DataSet");
        this.dataSetRepository = repository;
        this.dataSetFieldRepository = dataSetFieldRepository;
        this.dataSetMapper = dataSetMapper;
    }

    @Override
    public List<DataSetResponse> getDataSets() {
        return dataSetRepository.findByActiveTrueAndDisplayMainTrue().stream()
                .map(dataSetMapper::entityToResponse)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<DataSetFieldResponse> getDataSetFields(Long dataSetId) {
        return dataSetFieldRepository
                .findVisibleFieldsByDatasetId(dataSetId)
                .stream()
                .map(dataSetMapper::toFieldResponse).toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<TableRelationResponse> getRelations() {
        return dataSetRepository.findVisibleTableRelations().stream()
                .map(relation -> new TableRelationResponse(
                        relation.getSourceDatasetId(),
                        relation.getSourceTable(),
                        relation.getSourceDisplayName(),
                        relation.getSourceColumn(),
                        relation.getTargetDatasetId(),
                        relation.getTargetTable(),
                        relation.getTargetDisplayName(),
                        relation.getTargetColumn()
                ))
                .toList();
    }
}
