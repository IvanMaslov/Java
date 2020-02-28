package ru.ifmo.rain.maslov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

public class StudentDB implements AdvancedStudentGroupQuery {
    //TODO: inline
    private Comparator<Student> nameComparator =
            Comparator.comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName);
    private Comparator<Student> studentComparator =
            nameComparator.thenComparing(Student::getId);
    private Comparator<Group> groupComp =
            Comparator.comparing(Group::getName);
    private Function<Student, String> studentFullName = x -> x.getFirstName() + " " + x.getLastName();

    private List<String> getSome(List<Student> students, Function<Student, String> getter) {
        return students.stream()
                .map(getter)
                .collect(Collectors.toList());
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> comp) {
        return students.stream()
                .sorted(comp)
                .collect(Collectors.toList());
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> pred) {
        return students.stream()
                .filter(pred)
                .sorted(studentComparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getSome(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getSome(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getSome(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getSome(students, studentFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(getFirstNames(students));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Comparator.comparing(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, studentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, x -> name.equals(x.getFirstName()));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, x -> name.equals(x.getLastName()));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, x -> group.equals(x.getGroup()));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream()
                .filter(x -> group.equals(x.getGroup()))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        (a, b) -> a.compareTo(b) < 0 ? a : b
                ));
    }

    private List<Group> getGroupBy(Collection<Student> students, Comparator<Student> comp) {
        return students.stream()
                .sorted(comp)
                .collect(Collectors.groupingBy(
                        Student::getGroup,
                        Collectors.toList()))
                .entrySet()
                .stream()
                .map(x -> new Group(x.getKey(), x.getValue()))
                .sorted(groupComp)
                .collect(Collectors.toList());

    }

    private String getLargestGroupBy(Collection<Student> students, Comparator<Group> comp) {
        return getGroupsByName(students).stream()
                .max(comp)
                .map(Group::getName)
                .orElse("");
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupBy(students, studentComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupBy(students, Comparator.comparing(Student::getId));
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingInt(a -> a.getStudents().size()));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingLong(a ->
                a.getStudents()
                        .stream()
                        .map(Student::getFirstName)
                        .distinct()
                        .count()));
    }

    private List<String> getStudentById(Map<Integer, String> students,
                                        int[] indices) {
        return IntStream.of(indices)
                .boxed()
                .map(x -> students.getOrDefault(x, ""))
                .collect(Collectors.toList());
    }

    private Map<Integer, String> idMap(Collection<Student> students, Function<Student, String> property) {
        return students.stream().collect(Collectors.toMap(Student::getId, property));
    }

    private Map<String, Integer> nameMap(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(studentFullName,
                        Collectors.mapping(Student::getGroup,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size))));
    }

    private String getPriorityStudent(Collection<Student> students, Map<String, Integer> priority, Collection<Group> groups) {

            return students.stream()
                .max(Comparator.comparing(
                        (Student x) -> priority.getOrDefault(studentFullName.apply(x), -1))
                        .thenComparing(studentFullName))
                .map(studentFullName)
                .orElse("");
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return getPriorityStudent(students, nameMap(students), getGroupsByName(students));
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] indices) {
        return getStudentById(idMap(students, Student::getFirstName), indices);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] indices) {
        return getStudentById(idMap(students, Student::getLastName), indices);
    }

    @Override
    public List<String> getGroups(Collection<Student> students, int[] indices) {
        return getStudentById(idMap(students, Student::getGroup), indices);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] indices) {
        return getStudentById(idMap(students, studentFullName), indices);
    }
}

// java -cp . -p . -m info.kgeorgiy.java.advanced.student StudentGroupQuery  ru.ifmo.rain.maslov.student.StudentDB hello
