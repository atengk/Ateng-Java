package local.ateng.java.auth.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 存储用户的基本信息 实体类。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserPageBo implements Serializable {
    @NotNull(message = "分页号不能为空")
    @Min(value = 1, message = "分页号不正确")
    private Long pageNumber;
    @NotNull(message = "分页大小不能为空")
    @Min(value = 1, message = "分页大小不正确")
    private Long pageSize;
    private String nickName;
}
