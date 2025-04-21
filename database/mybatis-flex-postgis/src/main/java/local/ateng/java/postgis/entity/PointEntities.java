package local.ateng.java.postgis.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import local.ateng.java.postgis.handler.GeometryTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.postgis.jdbc.geometry.Geometry;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 *  实体类。
 *
 * @author ATeng
 * @since 2025-04-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("point_entities")
public class PointEntities implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Integer id;

    private String name;

    private String category;

    private Timestamp createdAt;

    @Column(typeHandler = GeometryTypeHandler.class)
    private Geometry geom;

}
