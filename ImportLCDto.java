package ru.lemanapro.nc.logisticconditions.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class ImportLCDto {
    TransitionData.GroupingKey key;
    List<TransitionData> records;
    Set<Long> productIds;
}
