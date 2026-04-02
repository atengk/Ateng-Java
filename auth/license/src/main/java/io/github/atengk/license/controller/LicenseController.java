package io.github.atengk.license.controller;

import io.github.atengk.license.model.License;
import io.github.atengk.license.service.LicenseService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * License 控制器
 *
 * 提供 License 导入、导出、查看、校验等功能
 */
@RestController
@RequestMapping("/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * 生成 License（后台使用）
     *
     * @param license License 参数
     * @return License 字符串
     */
    @PostMapping("/generate")
    public String generate(@RequestBody License license) {
        return licenseService.generate(license);
    }

    /**
     * 生成并导出 License（后台使用）
     *
     * @param license License 参数
     */
    @PostMapping("/generate/download")
    public void generateAndDownload(@RequestBody License license,
                                    HttpServletResponse response) {
        try {
            String licenseStr = licenseService.generate(license);

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=license.lic");

            response.getOutputStream().write(licenseStr.getBytes(StandardCharsets.UTF_8));
            response.getOutputStream().flush();

        } catch (Exception e) {
            throw new RuntimeException("生成 License 文件失败", e);
        }
    }

    /**
     * 生成机器码
     *
     * @return 机器码
     */
    @GetMapping("/generateMachineCode")
    public String generateMachineCode() {
        return licenseService.generateMachineCode();
    }

    /**
     * 导入 License 文件
     *
     * @param file License 文件
     * @return 是否成功
     */
    @PostMapping("/import")
    public boolean importLicense(@RequestParam("file") MultipartFile file) {
        try {
            licenseService.importLicense(file.getInputStream());
            return true;
        } catch (Exception e) {
            throw new RuntimeException("License 导入失败", e);
        }
    }

    /**
     * 导出 License 文件
     *
     * @param response 响应
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        try {
            byte[] data = licenseService.exportLicense();

            response.setContentType("application/octet-stream");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename=license.lic");

            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException("License 导出失败", e);
        }
    }

    /**
     * 获取当前 License 信息
     *
     * @return License 对象
     */
    @GetMapping("/current")
    public License current() {
        return licenseService.current();
    }

    /**
     * 校验当前 License（基础）
     *
     * @return 是否合法
     */
    @GetMapping("/validate")
    public boolean validate() {
        return licenseService.validate();
    }

    /**
     * 删除 License
     *
     * @return 是否成功
     */
    @DeleteMapping("/remove")
    public boolean remove() {
        licenseService.remove();
        return true;
    }
}
