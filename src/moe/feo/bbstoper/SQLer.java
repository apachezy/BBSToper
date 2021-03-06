package moe.feo.bbstoper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public interface SQLer {

	public default String getTableName(String name) {// 获取数据表应有的名字
		return Option.DATABASE_PREFIX.getString() + name;
	}

	public default void addPoster(Poster poster) {
		String sql = String.format(
				"INSERT INTO `%s` (`uuid`, `name`, `bbsname`, `binddate`, `rewardbefore`, `rewardtimes`) VALUES (?, ?, ?, ?, ?, ?);",
				getTableName("posters"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, poster.getUuid());
			pstmt.setString(2, poster.getName());
			pstmt.setString(3, poster.getBbsname());
			pstmt.setLong(4, poster.getBinddate());
			pstmt.setString(5, poster.getRewardbefore());
			pstmt.setInt(6, poster.getRewardtime());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public default void updatePoster(Poster poster) {
		String sql = String.format(
				"UPDATE `%s` SET `name`=?, `bbsname`=?, `binddate`=?, `rewardbefore`=?, `rewardtimes`=? WHERE `uuid`=?;",
				getTableName("posters"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, poster.getName());
			pstmt.setString(2, poster.getBbsname());
			pstmt.setLong(3, poster.getBinddate());
			pstmt.setString(4, poster.getRewardbefore());
			pstmt.setInt(5, poster.getRewardtime());
			pstmt.setString(6, poster.getUuid());
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public default void addTopState(String mcbbsname, String time) { // 记录一个顶贴
		String sql = String.format("INSERT INTO `%s` (`bbsname`, `time`) VALUES (?, ?);", getTableName("topstates"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, mcbbsname);
			pstmt.setString(2, time);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public default Poster getPoster(String uuid) {// 返回一个顶贴者
		String sql = String.format("SELECT * from `%s` WHERE `uuid`=?;", getTableName("posters"));
		PreparedStatement pstmt;
		Poster poster = null;
		try {
			pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, uuid);
			ResultSet rs = pstmt.executeQuery();
			try {
				if (rs.isClosed())
				return poster;
			} catch (AbstractMethodError e) {
			}
			
			if (rs.next()) {
				poster = new Poster();
				poster.setUuid(rs.getString("uuid"));
				poster.setName(rs.getString("name"));
				poster.setBbsname(rs.getString("bbsname"));
				poster.setBinddate(rs.getLong("binddate"));
				poster.setRewardbefore(rs.getString("rewardbefore"));
				poster.setRewardtime(rs.getInt("rewardtimes"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return poster;
	}

	public default List<String> getTopStatesFromPoster(Poster poster) {// 返回一个顶贴者的顶贴列表
		List<String> list = new ArrayList<String>();
		String sql = String.format("SELECT `time` from `%s` WHERE `bbsname`=?;", getTableName("topstates"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, poster.getBbsname());
			ResultSet rs = pstmt.executeQuery();
			try {
				if (rs.isClosed())
				return list;
			} catch (AbstractMethodError e) {
			}
			
			while (rs.next()) {
				list.add(rs.getString("time"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	public default String bbsNameCheck(String bbsname) {// 检查这个bbsname并返回一个uuid
		String sql = String.format("SELECT `uuid` from `%s` WHERE `bbsname`=?;", getTableName("posters"));
		String uuid = null;
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, bbsname);
			ResultSet rs = pstmt.executeQuery();
			try {
				if (rs.isClosed())// 如果查询是空的sqlite就会把结果关闭
				return uuid;
			} catch (AbstractMethodError e) {// 低版本没有这个特性
			}
			
			if (rs.next()) {// 但是mysql却会返回一个空结果集
				uuid = rs.getString("uuid");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return uuid;
	}

	public default boolean checkTopstate(String bbsname, String time) {// 查询是否存在这条记录，如果存在返回true，不存在返回false
		String sql = String.format("SELECT * FROM `%s` WHERE `bbsname`=? AND `time`=? LIMIT 1;",
				getTableName("topstates"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, bbsname);
			pstmt.setString(2, time);
			ResultSet rs = pstmt.executeQuery();
			try {
				if (rs.isClosed()) {// sqlite会关闭这个结果
					return false;
				}
			} catch (AbstractMethodError e) {// 但是低版本使用这个方法会报错
			}

			if (!rs.next()) {// mysql会返回一个空结果集，里面什么都没有
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public default List<Poster> getTopPosters() {// 按排名返回poster，并给poster写上count属性，不会返回没有顶过贴的玩家
		String sql = String.format("SELECT bbsname,COUNT(*) FROM `%s` GROUP BY bbsname ORDER BY COUNT(*) DESC;",
				getTableName("topstates"));
		List<Poster> list = new ArrayList<Poster>();
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String uuid = bbsNameCheck(rs.getString("bbsname"));
				Poster poster = getPoster(uuid);
				if (poster == null) continue;
				poster.setCount(rs.getInt("COUNT(*)"));
				list.add(poster);
			}
			return list;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public default List<Poster> getNoCountPosters() {// 由于上面的方法只会返回有顶贴的玩家
		String sql = String.format("SELECT * FROM `%s` WHERE `rewardbefore`='';", getTableName("posters"));
		PreparedStatement pstmt;
		List<Poster> posterlist = new ArrayList<Poster>();
		try {
			pstmt = getConnection().prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Poster poster = new Poster();
				poster.setUuid(rs.getString("uuid"));
				poster.setName(rs.getString("name"));
				poster.setBbsname(rs.getString("bbsname"));
				poster.setBinddate(rs.getLong("binddate"));
				poster.setRewardbefore(rs.getString("rewardbefore"));
				poster.setRewardtime(rs.getInt("rewardtimes"));
				poster.setCount(0);
				posterlist.add(poster);
			}
			return posterlist;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public default void deletePoster(String uuid) {
		String sql = String.format("DELETE FROM `%s` WHERE `uuid`=?;", getTableName("posters"));
		try {
			PreparedStatement pstmt = getConnection().prepareStatement(sql);
			pstmt.setString(1, uuid);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 获取当前sql的连接
	public Connection getConnection();

	// 关闭sql连接
	public void closeConnection();

	// 加载，插件启动时调用
	public void load();

}
