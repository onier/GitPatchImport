/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.springframework.data.elasticsearch.entities.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 目前不支持geo_shape
 *
 * @author xuzhenhai
 */
@Repository
public interface KernelPatchReposiroty extends ElasticsearchRepository<KernelPatch, String> {

}
