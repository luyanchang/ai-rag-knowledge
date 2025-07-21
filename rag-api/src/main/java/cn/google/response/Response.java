package cn.google.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 用于标准化API接口的返回格式，包含状态码、消息和数据
 *
 * @param <T> 响应数据的类型
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    /**
     * 响应状态码
     * 0000 表示成功，其他值表示失败
     */
    private String code;

    /**
     * 响应消息
     * 描述操作结果的详细信息
     */
    private String info;

    /**
     * 响应数据
     * 具体的业务数据内容
     */
    private T data;

}