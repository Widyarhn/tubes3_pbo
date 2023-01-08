package com.example.application.data.service;

import com.example.application.data.entity.Setting;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

    private final SettingRepository repository;

    public SettingService(SettingRepository repository) {
        this.repository = repository;
    }

    public Optional<Setting> get(Long id) {
        return repository.findById(id);
    }

    public Setting update(Setting entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<Setting> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Setting> list(Pageable pageable, Specification<Setting> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
