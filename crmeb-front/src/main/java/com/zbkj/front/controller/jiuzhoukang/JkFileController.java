package com.zbkj.front.controller.jiuzhoukang;

import com.zbkj.common.response.jiuzhoukang.JkFileObjectResponse;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.token.FrontTokenComponent;
import com.zbkj.service.service.jiuzhoukang.storage.JkFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/front/jk/file")
public class JkFileController {
    @Autowired private FrontTokenComponent token;
    @Autowired private JkFileStorageService storageService;

    private Long userId() { return Long.valueOf(token.getUserId()); }

    @PostMapping("/upload")
    public CommonResult<JkFileObjectResponse> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam String businessType,
                                                      @RequestParam(required = false) Long businessId) {
        return CommonResult.success(storageService.store(file, businessType, businessId, userId(), "PRIVATE"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        com.zbkj.common.model.jiuzhoukang.JkFileObject value = storageService.require(id);
        byte[] bytes = storageService.read(id, userId(), false);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(value.getContentType()));
        headers.setContentDispositionFormData("attachment", value.getOriginalName());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
