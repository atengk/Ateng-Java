package io.github.atengk.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件联系人信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailContact implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 联系人显示名称。
     */
    private String name;

    /**
     * 邮箱地址。
     */
    @Email(message = "邮箱地址格式不正确")
    @NotBlank(message = "邮箱地址不能为空")
    private String address;
}
